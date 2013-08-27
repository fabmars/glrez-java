/**
* Copyright 2005-2013 <a href="mailto:fabmars@gmail.com">Fabien Marsaud</a>
*
* Stripped version from the Demo project
* 
* This software is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*/
package com.chiptune.glrez;

import java.awt.Color;
import java.awt.event.KeyEvent;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.fixedfunc.GLLightingFunc;
import javax.swing.JOptionPane;

import org.mars.demo.graph.GLDemo;
import org.mars.toolkit.realtime.graph.FrameInfo;
import org.mars.toolkit.realtime.sound.ModulePlayer;

import com.chiptune.glrez.data.Resources;
import com.chiptune.glrez.scene.Scene;
import com.chiptune.glrez.scene.Zoom;


public abstract class GLRezHeadless extends GLDemo implements Runnable {

  private final boolean SNG = true; // music flag
  private final boolean MORE_KEYS = false; // cheatmode keys

  private TickTimer tickTimer;
  private ModulePlayer modPlayer;
  private boolean soundInitialized;
  private Scene scene;
  private boolean firstDraw = true; //Sometimes the module has played more than one row before the display kicks-in, so this fixes display errors on the first effect

  private float f_t1; // synchro time 1
  private float f_t2; // synchro time 2

  /* fog variable */
  private float f_v1; // level 1
  private float f_v2; // level 2
  private float f_n; // level (new)

  /* color variable */
  //private float m_r; // repeat ratio (unused in the C version)

  public GLRezHeadless() {
    tickTimer = new TickTimer(getTimer());
  }
  
  public final void initGraphAndSound(FrameInfo fi, ModulePlayer mp) throws Exception {
    initGraph(fi);
    initSound(mp);
  }

  public void initSound(ModulePlayer mp) throws Exception {
    modPlayer = mp;
    initSound();
  }

  @Override
  protected void initSound() throws Exception{
    if(isSoundEnabled()) {
      modPlayer.init();
    }
    soundInitialized = true;
  }

  protected boolean isSoundEnabled() {
    return SNG && (modPlayer != null);
  }

  @Override
  protected boolean isSoundInitialized() {
    return soundInitialized;
  }

  @Override
  protected void loadSound() {
    if(isSoundEnabled()) {
      modPlayer.load(Resources.modUrl);
    }
  }

  @Override
  protected void startSound() {
    if(isSoundEnabled()) {
      modPlayer.start();
    }
  }

  @Override
  protected void stopSound() {
    if(isSoundEnabled()) {
      modPlayer.stop();
    }
  }

  @Override
  protected void disposeSound() {
    if(isSoundEnabled()) {
      modPlayer.dispose();
    }
  }

  @Override
  protected void pauseSound() {
    if(isSoundEnabled()) {
      modPlayer.pause();
    }
  }

  
  /**
   * OpenGL setup post-drawable creation
   * Will be called upon windowed/full screen switch and the GL instance might have changed,
   * so all must be regenerated. 
   */
  @Override
  protected void drawableIntialized() throws Exception
  {
    gl.glShadeModel(GLLightingFunc.GL_SMOOTH); // enable smooth shading
    gl.glClearDepth(1.0f); // set depth buffer
    gl.glEnable(GL.GL_DEPTH_TEST); // enable depth testing
    gl.glDepthFunc(GL.GL_LEQUAL); // depth testing mode
    gl.glHint(GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST); // perspective calculations mode
    gl.glTexGeni(GL2.GL_S, GL2ES1.GL_TEXTURE_GEN_MODE, GL2.GL_SPHERE_MAP);// set mode S to sphere mapping
    gl.glTexGeni(GL2.GL_T, GL2ES1.GL_TEXTURE_GEN_MODE, GL2.GL_SPHERE_MAP);// set mode T to sphere mapping
    gl.glLineWidth(1.0f); // set line width
    
    // and reinit all the rest because the GL might have changed
    if(scene == null) {
      scene = new Scene(this);
    }
    scene.init();
  }

  @Override
  protected void drawableDisposed() throws Exception {
    scene.clear();
  }

  
  @Override
  protected void drawGLScene() {
    float t_g = tickTimer.getTime();

    // synchro
    if(isSoundEnabled()) {
      modPlayer.stream();
      //System.out.println(modPlayer);
      
      int ord = modPlayer.getOrd();
      int row = modPlayer.getRow();

      if (row == 0 || firstDraw) {
        firstDraw = false;
        
        switch (ord) {
        case 0:
          if (!scene.getMapping().isEnabled()) {
            scene.getMapping().setSynchroTime(t_g);
            scene.getMapping().setEnabled(true);
          }
          break;
          
        case 1:
          scene.getLiner().setEnabled(true);
          break;
          
        case 2:
          scene.getGlenz().setEnabled(true);
          boolean jumpEnabled = scene.getJump().isEnabled();
          scene.getRez().setEnabled(jumpEnabled);
          scene.getParticles().setEnabled(false);
          scene.getLiner().setEnabled(false);
          scene.getTitle().setEnabled(true);
          if (!scene.getZoom().isEnabled()) {
            scene.getZoom().setSynchroTime(t_g);
            scene.getZoom().setEnabled(true);
          }
          break;
          
        case 4:
          scene.getRez().setEnabled(true);
          scene.getParticles().setEnabled(true);
          scene.getLiner().setEnabled(true);
          if (!scene.getJump().isEnabled()) {
            scene.getJump().setSynchroTime(t_g);
            scene.getJump().setEnabled(true);
          }
          scene.getLiner().setTxtCredits();
          break;
          
        case 6:
          scene.getRez().setEnabled(false);
          scene.getGlenz().setEnabled(false);
          scene.getLiner().setTxtLoop();
          break;
        }
      }
      if (row % 8 == 0) {
        f_n = 0.25f;
        f_v2 = f_n;
        f_t2 = t_g;
      }
      if (row == 0) {
        f_n = 0.5f;
        f_v1 = f_n;
        f_t1 = t_g;
        f_v2 = 0;
      }
      if ((((row % 8 - 4) == 0) || (row == 60) || (row == 62)) && (ord > 1)) {
        scene.getCube().setJumpAngle(0.0f);
        scene.getCube().setJump(0.5f);
        scene.getCube().setSynchroTime(t_g);
      }
    }

    Color f_c = ColorUtils.safe(f_v1 + f_v2, f_v1 + f_v2, f_v1 + f_v2, 1.0f); // fog color
    if (f_v1 > 0) {
      f_v1 = (float) (f_n - f_n * 2.0 * (t_g - f_t1));
    }
    if (f_v2 > 0) {
      f_v2 = (float) (f_n - f_n * 2.0 * (t_g - f_t2));
    }
    
    // 3d mode
    scene.init3D();    
    scene.draw(f_c, t_g);
  }


  @Override
  protected void reshapeGL() {
    scene.init3D();
  }

  
  @Override
  public boolean dispatchKeyEvent(KeyEvent e) {
    if (!super.dispatchKeyEvent(e)) {
      int code = e.getKeyCode();
      float t_g = tickTimer.getTime();

      if (MORE_KEYS) {
        if (code == KeyEvent.VK_F2) { // F2 pressed ?
          scene.getRez().toggle(); // toggle on/off REZ
        }
        else if (code == KeyEvent.VK_F3) { // F3 pressed ?
          scene.getParticles().toggle(); // toggle on/off particles
        }
        else if (code == KeyEvent.VK_F4) { // F4 pressed ?
          scene.getGlenz().toggle(); // toggle on/off glenz
        }
        else if (code == KeyEvent.VK_F5) { // F5 pressed ?
          scene.getLiner().toggle(); // toggle on/off liner
        }
        else if (code == KeyEvent.VK_F6) { // F6 pressed ?
          scene.getTitle().toggle(); // toggle on/off title
        }
        else if (code == KeyEvent.VK_F7) { // F7 pressed ?
          scene.getMapping().toggle(); // toggle on/off mapping
          scene.getMapping().setSynchroTime(t_g);
        }
        else if (code == KeyEvent.VK_SPACE) { // space pressed ?
          scene.getCube().setJumpAngle(0.0f);
          scene.getCube().setJump(0.5f);
          scene.getCube().setSynchroTime(t_g);
          f_n = 0.25f;
          f_v2 = f_n;
          f_t2 = t_g;
        }
        else if (code == KeyEvent.VK_ENTER) { // return pressed ?
          scene.getJump().setEnabled(true); // move cube and title
          f_n = 0.5f;
          f_v1 = f_n;
          f_t1 = t_g;
          scene.getCube().setPosY(-3.85f);
          scene.getJump().setJumpY(3.85f);
          scene.getJump().setSynchroTime(t_g);
        }
        else if (code == KeyEvent.VK_TAB) { // tab pressed ?
          Zoom zoom = scene.getZoom();
          zoom.setEnabled(true); // move scene
          zoom.setZoom(10.0f);
          zoom.setMainAngle(0.0f);
          zoom.setSynchroTime(t_g);
        }
        else {
          return false;
        }
      }
    }

    return true;
  }
  
  /**
   * Is able to start in a separate thread in cases there would be locking issues with Swing
   */
  @Override
  public void run() {
    try {
      start();
    }
    catch (Throwable t) {
      t.printStackTrace();
      String msg = t.getMessage();
      if(msg == null || msg.length() == 0) {
        msg = t.getClass().getSimpleName();
      }
      JOptionPane.showMessageDialog(null, msg, "Oops!", JOptionPane.ERROR_MESSAGE);
    }
  }
}