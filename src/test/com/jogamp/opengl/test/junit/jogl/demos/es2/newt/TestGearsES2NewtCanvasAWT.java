/**
 * Copyright 2011 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
 
package com.jogamp.opengl.test.junit.jogl.demos.es2.newt;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.TextArea;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.common.os.Platform;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;

import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.InsetsImmutable;
import javax.media.nativewindow.util.Point;
import javax.media.nativewindow.util.PointImmutable;
import javax.media.nativewindow.util.DimensionImmutable;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLProfile;
import javax.swing.SwingUtilities;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

public class TestGearsES2NewtCanvasAWT extends UITestCase {    
    public enum FrameLayout { None, TextOnBottom, BorderBottom, BorderBottom2, BorderCenter, BorderCenterSurrounded, DoubleBorderCenterSurrounded };
    public enum ResizeBy { GLWindow, Component, Frame };
    
    static int screenIdx = 0;
    static PointImmutable wpos;
    static DimensionImmutable wsize, rwsize = null;
    static FrameLayout frameLayout = FrameLayout.None;
    static ResizeBy resizeBy = ResizeBy.Component;
    
    static long duration = 500; // ms
    static boolean opaque = true;
    static int forceAlpha = -1;
    static boolean fullscreen = false;
    static boolean pmvUseBackingArray = true;
    static int swapInterval = 1;
    static boolean showFPS = false;
    static int loops = 1;
    static boolean loop_shutdown = false;
    static boolean shallUseOffscreenFBOLayer = false;
    static boolean forceES2 = false;
    static boolean forceGL3 = false;
    static boolean mainRun = false;
    static boolean exclusiveContext = false;
    static boolean useAnimator = true;
    
    @BeforeClass
    public static void initClass() {
        if(null == wsize) {
            wsize = new Dimension(640, 480);
        }
    }

    @AfterClass
    public static void releaseClass() {
    }

    static void setGLWindowSize(final Frame frame, final GLWindow glw, final DimensionImmutable new_sz) {
        try {
            glw.setSize(new_sz.getWidth(), new_sz.getHeight());
            if( null != frame ) {
                javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        frame.pack();
                    } } );
            }
        } catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }       
    }
    static void setComponentSize(final Frame frame, final Component comp, final DimensionImmutable new_sz) {
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    java.awt.Dimension d = new java.awt.Dimension(new_sz.getWidth(), new_sz.getHeight());
                    comp.setMinimumSize(d);
                    comp.setPreferredSize(d);
                    comp.setSize(d);
                    if( null != frame ) {
                        frame.pack();
                    }
                } } );
        } catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }       
    }
    static void setFrameSize(final Frame frame, final boolean frameLayout, final DimensionImmutable new_sz) {
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    java.awt.Dimension d = new java.awt.Dimension(new_sz.getWidth(), new_sz.getHeight());
                    frame.setSize(d);
                    if( frameLayout ) {
                        frame.validate();
                    }
                } } );
        } catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }       
    }
    
    static void setSize(final ResizeBy resizeBy, final Frame frame, final boolean frameLayout, final Component comp, final GLWindow glw, final DimensionImmutable new_sz) {
        switch( resizeBy ) {
            case GLWindow:
                setGLWindowSize(frameLayout ? frame : null, glw, new_sz);
                break;
            case Component:
                setComponentSize(frameLayout ? frame : null, comp, new_sz);
                break;
            case Frame:
                setFrameSize(frame, frameLayout, new_sz);
                break;
        }        
    }
    
    // public enum ResizeBy { GLWindow, Component, Frame };
    protected void runTestGL(final GLCapabilitiesImmutable caps, final ResizeBy resizeBy, final FrameLayout frameLayout) throws InterruptedException, InvocationTargetException {
        System.err.println("requested: vsync "+swapInterval+", "+caps);
        Display dpy = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(dpy, screenIdx);
        final GLWindow glWindow = GLWindow.create(screen, caps);
        Assert.assertNotNull(glWindow);
        
        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow);
        if ( shallUseOffscreenFBOLayer ) {
            newtCanvasAWT.setShallUseOffscreenLayer(true);
        }
        
        final Frame frame = new Frame("AWT Parent Frame");
        
        setSize(resizeBy, frame, false, newtCanvasAWT, glWindow, wsize);

        switch( frameLayout) {
            case None:
                frame.add(newtCanvasAWT);
                break;
            case TextOnBottom:
                final TextArea ta = new TextArea(2, 20);
                ta.append("0123456789");
                ta.append(Platform.getNewline());
                ta.append("Some Text");
                ta.append(Platform.getNewline());
                frame.setLayout(new BorderLayout());
                frame.add(ta, BorderLayout.SOUTH);
                frame.add(newtCanvasAWT, BorderLayout.CENTER);
                break;                
            case BorderBottom:
                frame.setLayout(new BorderLayout());
                frame.add(newtCanvasAWT, BorderLayout.SOUTH);
                break;
            case BorderBottom2:
                frame.setLayout(new BorderLayout());
                frame.add(newtCanvasAWT, BorderLayout.SOUTH);
                frame.add(new Button("North"), BorderLayout.NORTH);
                break;
            case BorderCenter:
                frame.setLayout(new BorderLayout());
                frame.add(newtCanvasAWT, BorderLayout.CENTER);
                break;
            case BorderCenterSurrounded:
                frame.setLayout(new BorderLayout());
                frame.add(new Button("NORTH"), BorderLayout.NORTH);
                frame.add(new Button("SOUTH"), BorderLayout.SOUTH);
                frame.add(new Button("EAST"), BorderLayout.EAST);
                frame.add(new Button("WEST"), BorderLayout.WEST);
                frame.add(newtCanvasAWT, BorderLayout.CENTER);
                break;
            case DoubleBorderCenterSurrounded:
                Container c = new Container();
                c.setLayout(new BorderLayout());
                c.add(new Button("north"), BorderLayout.NORTH);
                c.add(new Button("south"), BorderLayout.SOUTH);
                c.add(new Button("east"), BorderLayout.EAST);
                c.add(new Button("west"), BorderLayout.WEST);
                c.add(newtCanvasAWT, BorderLayout.CENTER);
                
                frame.setLayout(new BorderLayout());
                frame.add(new Button("NORTH"), BorderLayout.NORTH);
                frame.add(new Button("SOUTH"), BorderLayout.SOUTH);
                frame.add(new Button("EAST"), BorderLayout.EAST);
                frame.add(new Button("WEST"), BorderLayout.WEST);
                frame.add(c, BorderLayout.CENTER);
                break;
        }
        
        frame.setTitle("Gears NewtCanvasAWT Test (translucent "+!caps.isBackgroundOpaque()+"), swapInterval "+swapInterval+", size "+wsize+", pos "+wpos);
        
        final GearsES2 demo = new GearsES2(swapInterval);
        demo.setPMVUseBackingArray(pmvUseBackingArray);
        glWindow.addGLEventListener(demo);
        
        final Animator animator = useAnimator ? new Animator() : null;
        if( useAnimator ) {
            animator.setModeBits(false, Animator.MODE_EXPECT_AWT_RENDERING_THREAD);
            animator.setExclusiveContext(exclusiveContext);
        }
        
        final QuitAdapter quitAdapter = new QuitAdapter();
        //glWindow.addKeyListener(new TraceKeyAdapter(quitAdapter));
        //glWindow.addWindowListener(new TraceWindowAdapter(quitAdapter));
        glWindow.addKeyListener(quitAdapter);
        glWindow.addWindowListener(quitAdapter);

        glWindow.addWindowListener(new WindowAdapter() {
            public void windowResized(WindowEvent e) {
                System.err.println("window resized: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight());
            }
            public void windowMoved(WindowEvent e) {
                System.err.println("window moved:   "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight());
            }            
        });
        
        glWindow.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                if(e.getKeyChar()=='f') {
                    quitAdapter.enable(false);
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set fullscreen  pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                            glWindow.setFullscreen(!glWindow.isFullscreen());
                            System.err.println("[set fullscreen post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                            glWindow.setExclusiveContextThread(t);
                            quitAdapter.clear();
                            quitAdapter.enable(true);
                    } }.start();
                } else if(e.getKeyChar()=='r') {
                    quitAdapter.enable(false);
                    if(glWindow.getParent()==null) {
                        System.err.println("XXX glWin to home");
                        glWindow.reparentWindow(newtCanvasAWT.getNativeWindow());
                    } else {
                        final InsetsImmutable nInsets = glWindow.getInsets();
                        java.awt.Insets aInsets = frame.getInsets();
                        System.err.println("XXX glWin to TOP - insets " + nInsets + ", " + aInsets);
                        glWindow.reparentWindow(null);
                        int dx, dy;
                        if(nInsets.getTotalHeight()==0) {
                            dx = aInsets.left;
                            dy = aInsets.top;
                        } else {
                            dx = nInsets.getLeftWidth();
                            dy = nInsets.getTopHeight();
                        }
                        glWindow.setPosition(frame.getX()+frame.getWidth()+dx, frame.getY()+dy);
                    }
                    glWindow.requestFocus();
                    quitAdapter.clear();
                    quitAdapter.enable(true);
                }
            }
        });

        if( useAnimator ) {
            animator.add(glWindow);
            animator.start();
            Assert.assertTrue(animator.isStarted());
            Assert.assertTrue(animator.isAnimating());
            Assert.assertEquals(exclusiveContext ? animator.getThread() : null, glWindow.getExclusiveContextThread());
        }

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               if( ResizeBy.Frame == resizeBy ) {
                   frame.validate();
               } else {
                   frame.pack();                   
               }                
               frame.setVisible(true);               
           }
        });        
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glWindow, true)); 
        
        if( useAnimator ) {
            animator.setUpdateFPSFrames(60, showFPS ? System.err : null);
        }
        
        System.err.println("NW chosen: "+glWindow.getDelegatedWindow().getChosenCapabilities());
        System.err.println("GL chosen: "+glWindow.getChosenCapabilities());
        System.err.println("window pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", "+glWindow.getInsets());
                
        if( null != rwsize ) {
            Thread.sleep(500); // 500ms delay 
            setSize(resizeBy, frame, true, newtCanvasAWT, glWindow, rwsize);
            System.err.println("window resize "+rwsize+" -> pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", "+glWindow.getInsets());
        }
        
        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(!quitAdapter.shouldQuit() && t1-t0<duration) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        if( useAnimator ) {
            Assert.assertEquals(exclusiveContext ? animator.getThread() : null, glWindow.getExclusiveContextThread());
            animator.stop();
            Assert.assertFalse(animator.isAnimating());
            Assert.assertFalse(animator.isStarted());
        }
        Assert.assertEquals(null, glWindow.getExclusiveContextThread());
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {               
               frame.dispose();               
           }
        });
        glWindow.destroy();
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glWindow, false));
    }

    @Test
    public void test01GL2ES2() throws InterruptedException, InvocationTargetException {
        for(int i=1; i<=loops; i++) {
            System.err.println("Loop "+i+"/"+loops);
            final GLProfile glp;
            if(forceGL3) {
                glp = GLProfile.get(GLProfile.GL3);
            } else if(forceES2) {
                glp = GLProfile.get(GLProfile.GLES2);
            } else {
                glp = GLProfile.getGL2ES2();
            }
            final GLCapabilities caps = new GLCapabilities( glp );
            caps.setBackgroundOpaque(opaque);
            if(-1 < forceAlpha) {
                caps.setAlphaBits(forceAlpha); 
            }
            runTestGL(caps, resizeBy, frameLayout);
            if(loop_shutdown) {
                GLProfile.shutdown();
            }
        }
    }

    @Test
    public void test02GL3() throws InterruptedException, InvocationTargetException {
        if(mainRun) return;
        
        if( !GLProfile.isAvailable(GLProfile.GL3) ) {
            System.err.println("GL3 n/a");
        }
        final GLProfile glp = GLProfile.get(GLProfile.GL3);
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, resizeBy, frameLayout);
    }
    
    public static void main(String args[]) throws IOException {
        mainRun = true;
        
        int x=0, y=0, w=640, h=480;
        int rw=-1, rh=-1;
        boolean usePos = false;
        
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-rwidth")) {
                i++;
                rw = MiscUtils.atoi(args[i], rw);
            } else if(args[i].equals("-rheight")) {
                i++;
                rh = MiscUtils.atoi(args[i], rh);
            } else if(args[i].equals("-layout")) {
                i++;
                frameLayout = FrameLayout.valueOf(args[i]);
            } else if(args[i].equals("-resizeBy")) {
                i++;
                resizeBy = ResizeBy.valueOf(args[i]);
            } else if(args[i].equals("-translucent")) {
                opaque = false;
            } else if(args[i].equals("-forceAlpha")) {
                i++;
                forceAlpha = MiscUtils.atoi(args[i], 0);
            } else if(args[i].equals("-fullscreen")) {
                fullscreen = true;
            } else if(args[i].equals("-pmvDirect")) {
                pmvUseBackingArray = false;
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-layeredFBO")) {
                shallUseOffscreenFBOLayer = true;
            } else if(args[i].equals("-exclctx")) {
                exclusiveContext = true;
            } else if(args[i].equals("-noanim")) {
                useAnimator  = false;
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-gl3")) {
                forceGL3 = true;
            } else if(args[i].equals("-showFPS")) {
                showFPS = true;
            } else if(args[i].equals("-width")) {
                i++;
                w = MiscUtils.atoi(args[i], w);
            } else if(args[i].equals("-height")) {
                i++;
                h = MiscUtils.atoi(args[i], h);
            } else if(args[i].equals("-x")) {
                i++;
                x = MiscUtils.atoi(args[i], x);
                usePos = true;
            } else if(args[i].equals("-y")) {
                i++;
                y = MiscUtils.atoi(args[i], y);
                usePos = true;
            } else if(args[i].equals("-screen")) {
                i++;
                screenIdx = MiscUtils.atoi(args[i], 0);
            } else if(args[i].equals("-loops")) {
                i++;
                loops = MiscUtils.atoi(args[i], 1);
            } else if(args[i].equals("-loop-shutdown")) {
                loop_shutdown = true;
            }
        }
        wsize = new Dimension(w, h);
        if( 0 < rw && 0 < rh ) {
            rwsize = new Dimension(rw, rh);
        }
        
        if(usePos) {
            wpos = new Point(x, y);
        }
        
        System.err.println("frameLayout "+frameLayout);
        System.err.println("resizeBy "+resizeBy);
        System.err.println("position "+wpos);
        System.err.println("size "+wsize);
        System.err.println("resize "+rwsize);
        System.err.println("screen "+screenIdx);
        System.err.println("translucent "+(!opaque));
        System.err.println("forceAlpha "+forceAlpha);        
        System.err.println("fullscreen "+fullscreen);
        System.err.println("pmvDirect "+(!pmvUseBackingArray));        
        System.err.println("loops "+loops);
        System.err.println("loop shutdown "+loop_shutdown);
        System.err.println("shallUseOffscreenFBOLayer     "+shallUseOffscreenFBOLayer);
        System.err.println("forceES2 "+forceES2);
        System.err.println("forceGL3 "+forceGL3);
        System.err.println("swapInterval "+swapInterval);
        System.err.println("exclusiveContext "+exclusiveContext);
        System.err.println("useAnimator "+useAnimator);

        org.junit.runner.JUnitCore.main(TestGearsES2NewtCanvasAWT.class.getName());
    }
}
