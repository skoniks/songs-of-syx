package snake2d;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.Version;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWNativeCocoa;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.JNI;
import org.lwjgl.system.macosx.ObjCRuntime;

import launcher.LSettings;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.datatypes.Coo;
import snake2d.util.file.FileManager;
import snake2d.util.file.SnakeImage;
import snake2d.util.misc.OS;

public class GraphicContext {

  public final int nativeWidth;
  public final int nativeHeight;
  public final int displayWidth;
  public final int displayHeight;
  public final COORDINATE blitArea;
  private boolean windowIsFocused = true;
  final int refreshRate;
  private final GlHelper gl;
  private final long window;
  final Renderer renderer;
  final String screenShotPath;
  static boolean diagnosing = false;
  private static int diagnoseTimer = 0;
  private final boolean debugAll;
  private TextureHolder texture;
  int chi = 0;
  int bi = -1;

  GraphicContext(SETTINGS sett) {
    // LSettings ls = getLSettings();
    // int borderLess = LSettings.screenModeBorderLess;
    // Printer.ln(ls.screenMode.get());
    // System.exit(0);

    String icons;
    boolean fullscreen;
    this.debugAll = sett.debugMode();
    Configuration.DEBUG.set(this.debugAll);
    Configuration.DEBUG_STREAM.set(System.out);
    Configuration.DEBUG_MEMORY_ALLOCATOR.set(this.debugAll);
    Configuration.DEBUG_STACK.set(this.debugAll);
    Error error = new Error();
    if (sett.getScreenshotFolder() != null) {
      this.screenShotPath = sett.getScreenshotFolder();
    } else {
      File f = new File("screenshots");
      if (f.exists() && !f.isDirectory()) {
        f.delete();
      }
      if (!f.exists()) {
        f.mkdirs();
      }
      this.screenShotPath = String.valueOf(f.getAbsolutePath()) + File.separator;
    }
    if (sett.getPointSize() != 1 && sett.getPointSize() % 2 != 0) {
      throw new RuntimeException("pointsize must be a power of two!");
    }
    if (this.debugAll) {
      GLFWErrorCallback.createPrint(System.out).set();
    }
    if (!GLFW.glfwInit()) {
      throw new IllegalStateException("Unable to initialize GLFW");
    }
    boolean mac = OS.get() == OS.MAC;
    boolean dec = mac || sett.decoratedWindow();
    GLFW.glfwDefaultWindowHints();
    GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, mac ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_DECORATED, dec ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_FOCUSED, GLFW.GLFW_TRUE);
    GLFW.glfwWindowHint(GLFW.GLFW_FOCUS_ON_SHOW, GLFW.GLFW_TRUE);
    GLFW.glfwWindowHint(GLFW.GLFW_FLOATING, sett.windowFloating() ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_RED_BITS, 8);
    GLFW.glfwWindowHint(GLFW.GLFW_GREEN_BITS, 8);
    GLFW.glfwWindowHint(GLFW.GLFW_BLUE_BITS, 8);
    GLFW.glfwWindowHint(GLFW.GLFW_ALPHA_BITS, 8);
    GLFW.glfwWindowHint(GLFW.GLFW_DEPTH_BITS, 0);
    GLFW.glfwWindowHint(GLFW.GLFW_STENCIL_BITS, 0);
    GLFW.glfwWindowHint(GLFW.GLFW_SAMPLES, 0);
    GLFW.glfwWindowHint(GLFW.GLFW_REFRESH_RATE, -1);
    GLFW.glfwWindowHint(GLFW.GLFW_STEREO, GLFW.GLFW_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_SRGB_CAPABLE, GLFW.GLFW_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_DOUBLEBUFFER, GLFW.GLFW_TRUE);
    GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_ROBUSTNESS, 0);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_RELEASE_BEHAVIOR, 0);
    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_DEBUG, this.debugAll ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_SCALE_FRAMEBUFFER, GLFW.GLFW_TRUE);
    GLFW.glfwWindowHint(GLFW.GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW.GLFW_FALSE);
    GLFW.glfwWindowHint(GLFW.GLFW_AUTO_ICONIFY, sett.autoIconify() ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
    new Displays();
    this.printSettings(sett);
    Printer.ln("GRAPHICS");
    Displays.DisplayMode wanted = sett.display();
    Printer.ln("WANTED: " + wanted + ", " + (wanted.fullScreen ? "fullscreen" : "windowed"));
    long pointer = Displays.pointer(sett.monitor());
    int dispWidth = wanted.width;
    int dispHeight = wanted.height;
    this.nativeWidth = sett.getNativeWidth();
    this.nativeHeight = sett.getNativeHeight();
    this.refreshRate = wanted.refresh;
    GLFW.glfwWindowHint(GLFW.GLFW_REFRESH_RATE, this.refreshRate);
    Displays.DisplayMode current = Displays.current(sett.monitor());
    Printer.ln("CURRENT: " + current + ", " + (current.fullScreen ? "fullscreen" : "windowed"));
    if (!(wanted.fullScreen || dispWidth <= current.width && dispHeight <= current.height)) {
      dispWidth = current.width;
      dispHeight = current.height;
    }
    fullscreen = wanted.fullScreen || dispWidth == current.width && dispHeight == current.height;
    if (fullscreen && sett.windowFullFull()) {
      fullscreen = false;
      IntBuffer wx = BufferUtils.createIntBuffer(1);
      IntBuffer wy = BufferUtils.createIntBuffer(1);
      IntBuffer ww = BufferUtils.createIntBuffer(1);
      IntBuffer wh = BufferUtils.createIntBuffer(1);
      GLFW.glfwGetMonitorWorkarea(pointer, wx, wy, ww, wh);
      dispWidth = ww.get();
      dispHeight = wh.get();
    }
    this.displayWidth = dispWidth;
    this.displayHeight = dispHeight;
    if (fullscreen) {
      GLFWVidMode vm = GLFW.glfwGetVideoMode(pointer);
      GLFW.glfwWindowHint(GLFW.GLFW_RED_BITS, vm.redBits());
      GLFW.glfwWindowHint(GLFW.GLFW_GREEN_BITS, vm.greenBits());
      GLFW.glfwWindowHint(GLFW.GLFW_BLUE_BITS, vm.blueBits());
      GLFW.glfwWindowHint(GLFW.GLFW_REFRESH_RATE, vm.refreshRate());
    }
    try {
      Printer.ln(
          "---attempting resolution: " + this.displayWidth + "x" + this.displayHeight + ", " + this.refreshRate + "Hz, "
              + (fullscreen ? (wanted.fullScreen ? "fullscreen" : "borderless") : "windowed") + ", monitor "
              + sett.monitor() + " (" + GLFW.glfwGetMonitorName(pointer) + ")");
      this.window = GLFW.glfwCreateWindow(this.displayWidth, this.displayHeight, sett.getWindowName(),
          fullscreen ? pointer : 0L, 0L);
    } catch (Exception e) {
      e.printStackTrace();
      throw error.get("window create " + e);
    }
    if (this.window == 0L) {
      throw error.get("window is null");
    }
    int[] dx = new int[1];
    int[] dy = new int[1];
    GLFW.glfwGetMonitorPos(pointer, dx, dy);
    if (!fullscreen && dec) {
      int x1 = (Displays.current((int) sett.monitor()).width - this.displayWidth) / 4;
      int y1 = (Displays.current((int) sett.monitor()).height - this.displayHeight) / 4;
      if (x1 < 0) {
        x1 = 0;
      }
      if (y1 < 0) {
        y1 = 0;
      }
      if (dec) {
        y1 += 30;
      }
      GLFW.glfwSetWindowPos(this.window, x1 + dx[0], y1 + dy[0]);
    }
    if ((icons = sett.getIconFolder()) != null) {
      _IconLoader.setIcon(this.window, icons);
    } else {
      Printer.ln("NOTE: no icon-folder specified");
    }
    try {
      GLFW.glfwMakeContextCurrent(this.window);
    } catch (Exception e) {
      e.printStackTrace();
      throw error.get("make current " + e);
    }
    long monitor = GLFW.glfwGetWindowMonitor(this.window);
    if (monitor == 0L) {
      monitor = GLFW.glfwGetPrimaryMonitor();
    }
    GLFWVidMode vidMode = GLFW.glfwGetVideoMode(monitor);
    Printer.ln("---Setting FPS to " + vidMode.refreshRate());
    int swapInterval = 0;
    if (sett.getVSynchEnabled() || sett.vsyncAdaptive()) {
      swapInterval = 1;
      int r = this.refreshRate;
      while (vidMode.refreshRate() >= r * 2) {
        r *= 2;
        ++swapInterval;
      }
      if (sett.vsyncAdaptive() && (GLFW.glfwExtensionSupported("WGL_EXT_swap_control_tear")
          || GLFW.glfwExtensionSupported("GLX_EXT_swap_control_tear"))) {
        Printer.ln("---'Adaptive' Vsync enabled (" + swapInterval + ")");
        swapInterval *= -1;
      }
    }
    GLFW.glfwSwapInterval(swapInterval);
    Printer.ln("---created resolution: " + vidMode.width() + "x" + vidMode.height() + ", " + vidMode.refreshRate()
        + "Hz" + (sett.getVSynchEnabled() ? ", vsync: " + swapInterval : ""));
    Printer.ln("---LWJGL: " + Version.getVersion());
    Printer.ln("---GLFW: " + GLFW.glfwGetVersionString());
    this.gl = new GlHelper(sett.getNativeWidth(), sett.getNativeHeight(), this.debugAll);
    if (!GL.getCapabilities().OpenGL33) {
      throw error.get("gl Capabilities");
    }
    if (OS.get() == OS.MAC) {
      this.blitArea = new Coo(GlHelper.FBSize());
    } else {
      IntBuffer w = BufferUtils.createIntBuffer(1);
      IntBuffer h = BufferUtils.createIntBuffer(1);
      GLFW.glfwGetFramebufferSize(this.window, w, h);
      Coo sc = new Coo(w.get(), h.get());
      this.blitArea = new Coo(sc.x(), sc.y());
    }
    Printer.ln("---BLIT: " + this.blitArea.x() + "x" + this.blitArea.y());
    Printer.fin();
    switch (sett.getRenderMode()) {
      case 0: {
        this.renderer = new RendererDebug(sett, sett.getPointSize());
        break;
      }
      default: {
        this.renderer = new RendererDeffered(sett, sett.getPointSize());
      }
    }
    GLFW.glfwFocusWindow(this.window);
    GlHelper.checkErrors();
    // System.exit(0);
  }

  private void macToggleNativeFullscreen() {
    if (OS.get() != OS.MAC)
      return;

    Printer.ln("Attempting to toggle native fullscreen on Mac...");

    // NSWindow* из GLFW
    long nsWindow = GLFWNativeCocoa.glfwGetCocoaWindow(this.window);
    if (nsWindow == 0L) {
      Printer.ln("Cocoa: nsWindow == 0 (can't toggle fullscreen)");
      return;
    }

    // selector: toggleFullScreen:
    long selToggle = ObjCRuntime.sel_registerName("toggleFullScreen:");

    // В Objective-C метод принимает sender (id), можно передать nil
    // Важно: используем invokePPV (receiver, selector, arg) -> void
    JNI.invokePPV(nsWindow, selToggle, ObjCRuntime.nil, ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend"));
  }

  public String render() {
    return GlHelper.renderer;
  }

  public String renderV() {
    return GlHelper.rendererV;
  }

  private void printSettings(SETTINGS sett) {
    Printer.ln("SETTINGS");
    Printer.ln("Debug: " + sett.debugMode());
    Printer.ln("Native Screen: " + sett.getNativeWidth() + "x" + sett.getNativeHeight());
    Printer.ln("Display: " + sett.display());
    Printer.ln("Full: " + sett.display().fullScreen);
    Printer.ln("Mode: " + sett.getRenderMode());
    Printer.ln("Fit: " + sett.getFitToScreen());
    Printer.ln("Linear: " + sett.getLinearFiltering());
    Printer.ln("VSync: " + sett.getVSynchEnabled());
    Printer.fin();
  }

  void makeVisable() {
    GLFW.glfwShowWindow(this.window);
    GLFW.glfwFocusWindow(this.window);
    macToggleNativeFullscreen();
  }

  final void setTexture(TextureHolder texture) {
    this.texture = texture;
  }

  void flushRenderer() {
    this.renderer.flush();
    if (this.texture != null) {
      this.texture.flush();
    }
  }

  boolean swapAndCheckClose() {
    if (this.debugAll && (this.chi & 0xFF) == 0) {
      GlHelper.checkErrors();
    }
    if (this.bi == -1) {
      this.bi = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
    }
    GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, 0);
    GLFW.glfwSwapBuffers(this.window);
    GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.bi);
    this.windowIsFocused = GLFW.glfwGetWindowAttrib(this.window, GLFW.GLFW_FOCUSED) == 1;
    this.diagnose(false);
    if (this.debugAll && (this.chi & 0xFF) == 0) {
      GlHelper.checkErrors();
    }
    ++this.chi;
    return !GLFW.glfwWindowShouldClose(this.window);
  }

  private void diagnose(boolean force) {
    if (diagnosing) {
      if (force) {
        Printer.ln("force");
      }
      if (++diagnoseTimer == 60 || force) {
        diagnoseTimer = 0;
        GlHelper.diagnozeMem();
      }
      GlHelper.checkErrors();
    }
  }

  public boolean focused() {
    return this.windowIsFocused;
  }

  void pollEvents() {
    GLFW.glfwPollEvents();
  }

  long getWindow() {
    return this.window;
  }

  void dis() {
    if (this.renderer != null) {
      this.renderer.dis();
    }
    this.gl.dispose();
    Callbacks.glfwFreeCallbacks(this.window);
    GLFW.glfwDestroyWindow(this.window);
    GLFW.glfwTerminate();
    GLFWErrorCallback e = GLFW.glfwSetErrorCallback(null);
    if (e != null) {
      e.free();
    }
    Printer.ln(GraphicContext.class + " was sucessfully destroyed");
  }

  static void terminate() {
  }

  boolean isFocused() {
    return this.windowIsFocused;
  }

  void takeScreenShot() {
    String s = FileManager.NAME.timeStampString(String.valueOf(this.screenShotPath) + "shot");
    SnakeImage image = new SnakeImage(this.nativeWidth, this.nativeHeight);
    this.copyFB(image, 0, 0);
    image.save(String.valueOf(s) + ".png");
    System.gc();
  }

  public void makeScreenShot() {
    if (this.screenShotPath == null) {
      return;
    }
    new CORE.GlJob() {

      @Override
      protected void doJob() {
        GraphicContext.this.takeScreenShot();
      }
    }.perform();
  }

  void copyFB(SnakeImage image, int startX, int startY) {
    ByteBuffer buff = this.gl.getFramePixels(this.nativeWidth, this.nativeHeight);
    int x = 0;
    while (x < this.nativeWidth) {
      int x1 = startX + x;
      if (x1 < image.width) {
        int y = 0;
        while (y < this.nativeHeight) {
          int y1 = startY + this.nativeHeight - (y + 1);
          if (y1 < image.height) {
            int i = (x + this.nativeWidth * y) * 4;
            int r = buff.get(i) & 0xFF;
            int g = buff.get(i + 1) & 0xFF;
            int b = buff.get(i + 2) & 0xFF;
            image.rgb.set(x1, y1, r, g, b, 255);
          }
          ++y;
        }
      }
      ++x;
    }
  }

  void copyFB(SnakeImage image, int startX, int startY, int scale) {
    ByteBuffer buff = this.gl.getFramePixels(this.nativeWidth, this.nativeHeight);
    int x = 0;
    while (x < this.nativeWidth / scale) {
      int x1 = startX + x;
      if (x1 < image.width) {
        int y = 0;
        while (y < this.nativeHeight / scale) {
          int y1 = startY + this.nativeHeight / scale - (y + 1);
          if (y1 < image.height) {
            int i = (x * scale + this.nativeWidth * y * scale) * 4;
            int r = buff.get(i) & 0xFF;
            int g = buff.get(i + 1) & 0xFF;
            int b = buff.get(i + 2) & 0xFF;
            image.rgb.set(x1, y1, r, g, b, 255);
          }
          ++y;
        }
      }
      ++x;
    }
  }

  private class Error {
    private String mess;

    public Error() {
      GraphicsCardGetter g = new GraphicsCardGetter();
      this.mess = "The game failed setting up openGl on your graphics card. This is likeley because your graphics card has no opengl 3.3 support. Some PC's have multiple graphics cards. In this case, try configuring the app to use the other.graphics card in graphic card's control panel. (You may need to do this for java as well.) ";
      this.mess = String.valueOf(this.mess) + System.lineSeparator();
      this.mess = String.valueOf(this.mess) + "Current graphics card: ";
      this.mess = String.valueOf(this.mess) + g.version();
      this.mess = String.valueOf(this.mess) + System.lineSeparator();
      this.mess = String.valueOf(this.mess) + System.lineSeparator();
      this.mess = String.valueOf(this.mess)
          + "If your graphics card does not support opengl 3.3 or higher, please do not report this error.";
      if (g.version() == null) {
        throw this.get("version");
      }
    }

    Errors.GameError get(String message) {
      return new Errors.GameError(String.valueOf(this.mess) + System.lineSeparator() + message);
    }
  }

  //

  private LSettings getLSettings() {
    try {
      Object s = init.settings.S.get();
      Class<? extends Object> c = s.getClass();
      Field f = c.getDeclaredField("settings");
      f.setAccessible(true);
      return (LSettings) f.get(s);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Can't access S.settings", e);
    }
  }
}
