package ch.irb.IgGenealogicTreeMaker;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

@SuppressWarnings("serial")
public class ProgressBarFrame extends JFrame {
	

	public ProgressBarFrame(Component parentComponent){
        super("Processing program...");
        
		try {
			java.net.URL url = ClassLoader.getSystemResource("ch/irb/IgGenealogicTreeMaker/resources/icon.png");
			Toolkit kit = Toolkit.getDefaultToolkit();
			Image img = kit.createImage(url);
			this.setIconImage(img);
		} catch (Exception e) {
			e.printStackTrace();
		}
		//Create and set up the window.

        setLocationRelativeTo(parentComponent);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(new Dimension(400, 100));
 
        //Create and set up the content pane.
        JComponent newContentPane = new ProgressBarPanel();
        newContentPane.setOpaque(true); //content panes must be opaque
       setContentPane(newContentPane);
 
        //Display the window.
        setVisible(true);
	}
	
	public void close(){
	}
	
	public class ProgressBarPanel extends JPanel implements PropertyChangeListener {

		private JProgressBar progressBar;
		private Task task;
		
		

		class Task extends SwingWorker<Void, Void> {
			 private static final long SLEEP_TIME = 4000;
			    private String text;

			    public Task(String text) {
			        this.text = text;
			    }

			    @Override
			    public Void doInBackground() {
			        setProgress(0);
			        try {
			            Thread.sleep(SLEEP_TIME);// imitate a long-running task
			        } catch (InterruptedException e) {
			        }
			        setProgress(100);
			        return null;
			    }

			    @Override
			    public void done() {
			        System.out.println(text + " is done");
			        Toolkit.getDefaultToolkit().beep();
			    }
		}

		public ProgressBarPanel() {
			super(new BorderLayout());

			progressBar = new JProgressBar(0, 100);
			progressBar.setValue(0);

			// Call setStringPainted now so that the progress bar height
			// stays the same whether or not the string is shown.
			progressBar.setStringPainted(true);

			//we launch the task
			progressBar.setIndeterminate(true);
			// Instances of javax.swing.SwingWorker are not reusuable, so
			// we create new instances as needed.
			task = new Task("test");
			task.addPropertyChangeListener(this);
			task.execute();
			

			add(progressBar, BorderLayout.CENTER);
			setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		}


		/**
		 * Invoked when task's progress property changes.
		 */
		public void propertyChange(PropertyChangeEvent evt) {
			if ("progress" == evt.getPropertyName()) {
				int progress = (Integer) evt.getNewValue();
				progressBar.setIndeterminate(false);
				progressBar.setValue(progress);
			}
		}


	}
}
