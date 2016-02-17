/* GameLoop
Brett Binnersley, V00776751

This file controls the main gameloop, performing all the events and the 'main' logic.
It contains all the objects in the scene at any given time, and runs the events on them
when they are issued.
*/

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseWheelEvent;
import javax.swing.Action;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.HashSet;

class GameLoop extends JComponent {

	public enum KeyState {
		KEYPRESS,
		KEYHELD,
		KEYRELEASE
	}

	public enum MouseState {
		MOUSEPRESS,
		MOUSEHELD,
		MOUSERELEASE
	}

	public GameLoop(int width, int height) {
    canvas_width = width;
    canvas_height = height;
		gameobjects = new HashMap<Integer, GameObject>();  // Objects in game
		keyStates = new ConcurrentHashMap<Integer, KeyState>();
		mouseStates = new ConcurrentHashMap<Integer, MouseState>();

		for (int i=0; i<10; ++i) {
			Enemy e = new Enemy((int)(Math.random() * (double)width), (int)(Math.random() * (double)height));
			gameobjects.put(e.id, e);
		}
		Player p = new Player(200, 200);
		gameobjects.put(p.id, p);
		setDoubleBuffered(true);
		setSize(width, height);

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				HandleMousePress(x,y,e.getButton());
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				HandleMouseRelease(x,y,e.getButton());
			}
		});

		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				HandleMouseMove(x,y);
			}
		});

    addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				HandleKeyDown(e);
			}
			@Override
			public void keyReleased(KeyEvent e) {
				HandleKeyUp(e);
			}
		});

		setFocusable(true);
		requestFocusInWindow();
	}


	private int canvasWidth() {
		return canvas_width;
	}

	private int canvasHeight() {
		return canvas_height;
	}

	private void HandleMouseMove(int x, int y) {
		if (x < 0 || x >= canvasWidth()) {
      return;
    }
		if (y < 0 || y >= canvasHeight()) {
      return;
    }
		Mouse.SetMouseX(x);
		Mouse.SetMouseY(y);
	}

	private void HandleMousePress(int x, int y, int button_number) {
		if (x < 0 || x >= canvasWidth()) {
      return;
    }
		if (y < 0 || y >= canvasHeight()) {
      return;
    }
		if (keyStates.containsKey(button_number)) {
			mouseStates.replace(button_number, MouseState.MOUSEPRESS);
		} else {
			mouseStates.put(button_number, MouseState.MOUSEPRESS);
		}
	}

	private void HandleMouseRelease(int x, int y, int button_number) {
		if (x < 0 || x >= canvasWidth()) {
      return;
    }
		if (y < 0 || y >= canvasHeight()) {
      return;
    }
		if (keyStates.containsKey(button_number)) {
			mouseStates.replace(button_number, MouseState.MOUSERELEASE);
		} else {
			mouseStates.put(button_number, MouseState.MOUSERELEASE);
		}
	}

	private void HandleKeyDown(KeyEvent e) {
		int keyCode = e.getKeyCode();
		if (keyStates.containsKey(keyCode)) {
			keyStates.replace(keyCode, KeyState.KEYPRESS);
		} else {
			keyStates.put(keyCode, KeyState.KEYPRESS);
		}
	}

	private void HandleKeyUp(KeyEvent e) {
		int keyCode = e.getKeyCode();
		if (keyStates.containsKey(keyCode)) {
			keyStates.replace(keyCode, KeyState.KEYRELEASE);
		} else {
			keyStates.put(keyCode, KeyState.KEYRELEASE);
		}
	}

	// Handle all the keyboard and mouse events (including persistent events, IE: HELD).
	// Also handle safely removing objects from the scene.
	private void RunEvents() {

		Collection<GameObject> allObjects = gameobjects.values();

		// Handle keyboard events for all of the objects.
		for (HashMap.Entry<Integer, KeyState> t_state : keyStates.entrySet()) {
			int key = t_state.getKey();
			KeyState state = t_state.getValue();
			if (state == KeyState.KEYPRESS) {
				for (GameObject obj : allObjects) {
					obj.HandleKeyPress(key);
				}
				keyStates.replace(key, KeyState.KEYHELD);
			}
			else if (state == KeyState.KEYHELD) {
				for (GameObject obj : allObjects) {
					obj.HandleKeyDown(key);
				}
			}
			else if (state == KeyState.KEYRELEASE) {
				for (GameObject obj : allObjects) {
					obj.HandleKeyRelease(key);
				}
				keyStates.remove(key);
			} else {
				System.out.println("Error: Unknown Keystate");
			}
		}

		// Handle Mouse Events for all the objects
		for (HashMap.Entry<Integer, MouseState> t_state : mouseStates.entrySet()) {
			int key = t_state.getKey();
			MouseState state = t_state.getValue();
			if (state == MouseState.MOUSEPRESS) {
				for (GameObject obj : allObjects) {
					obj.HandleMousePress(key);
				}
				mouseStates.replace(key, MouseState.MOUSEHELD);
			}
			else if (state == MouseState.MOUSEHELD) {
				for (GameObject obj : allObjects) {
					obj.HandleMouseHeld(key);
				}
			}
			else if (state == MouseState.MOUSERELEASE) {
				for (GameObject obj : allObjects) {
					obj.HandleMouseRelease(key);
				}
				mouseStates.remove(key);
			} else {
				System.out.println("Error: Unknown MouseState");
			}
		}

		// Step every object and handle key events
		for (GameObject obj : allObjects) {
			obj.LogicStep();
		}

		// Remove every object flagged for deletion
		ArrayList<Integer> deleteObjects = new ArrayList<Integer>();
		for (GameObject obj : allObjects) {
			if (obj.IsFlaggedDeleted()) {
				deleteObjects.add(obj.id);
			}
		}
		for (Integer key : deleteObjects) {
			gameobjects.remove(key);
		}
		deleteObjects.clear();
	}

	// Draw all the objects that still exist in the game.
	@Override
	public void paintComponent(Graphics g) {
		// Run all the events for every object.
		RunEvents();

		// Render the component
		super.paintComponent(g);
		Graphics2D g2d = (Graphics2D)g;
		g2d.setColor(new Color(128,128,128));
		g2d.fillRect(0, 0, canvas_width, canvas_height);
		for (GameObject obj : gameobjects.values()) {
			obj.Render(g2d);
		}
	}

	public void RunLogicDrawEntities(double frame_delta_ms) {
		repaint();
	}

  private int canvas_width, canvas_height;
	private HashMap<Integer, GameObject> gameobjects;
	private ConcurrentHashMap<Integer, KeyState> keyStates;
	private ConcurrentHashMap<Integer, MouseState> mouseStates;
}
