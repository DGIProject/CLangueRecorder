package fr.gv;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;

import netscape.javascript.JSObject;


public class AudioRecorder extends JApplet implements ActionListener,
        ChangeListener, ItemListener {

    private static final long serialVersionUID = -8510080016790233199L;
    // Global declarations
    protected boolean running;
    ByteArrayOutputStream out = null;
    AudioFileFormat.Type fileType;
    Object lock = new Object();
    TargetDataLine line = null;
    SourceDataLine sline = null;
    volatile boolean paused = false;
    String FileNameRecord; 
    boolean first;
    JSObject jso;
    
    JButton record;
    JButton play;
    JButton stop;
    JButton send;
    JButton open_file;
    JLabel choosed_file;
    JLabel uploadState;
    JLabel ImageLabel;
    JProgressBar progressBar;
    boolean file_chosed = false;
    String work, User;
    
    public void init() {
    	
    	System.setSecurityManager(null); 
        setLayout(null);    
        jso = JSObject.getWindow(this);
        
        String test = "Inited";
        
        if(jso != null )
        {
            try {
                jso.call("changeStatue", new String[] {test});
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            
        }
        
        String path = System.getProperty("user.home");
        FileNameRecord = path+"\\record.wav";
        work = getParameter("W");
        User = getParameter("U");
        System.out.println(FileNameRecord);

        
        record = new JButton("Enregistrer");
        play = new JButton("Ecouter");
        stop = new JButton("Arrêter");
        send = new JButton("Envoyer");
        open_file = new JButton("Parcourir");
        progressBar = new JProgressBar();
        choosed_file = new JLabel();
        uploadState = new JLabel();
        ImageLabel = new JLabel();

        record.setBounds(70, 10, 100, 25);
        play.setBounds(175, 10, 80, 25);
        stop.setBounds(260, 10, 80, 25);
        send.setBounds(70, 50, 80, 25);
        open_file.setBounds(70, 80, 100, 25);
        choosed_file.setBounds(200, 80, 200, 25);
        uploadState.setBounds(200, 30, 200, 50);        
        progressBar.setBounds(200, 80, 200, 25);

        add(record);
        add(play);
        add(stop);
        add(send);
        add(open_file);
        add(choosed_file);
        add(uploadState);
        add(ImageLabel);
        add(progressBar);

        record.setEnabled(true);
        play.setEnabled(true);
        stop.setEnabled(true);
        send.setEnabled(true);
        open_file.setEnabled(true);
        
        progressBar.setIndeterminate(true);
        progressBar.hide();


        record.addActionListener(this);
        play.addActionListener(this);
        stop.addActionListener(this);
        send.addActionListener(this);
        open_file.addActionListener(this);

    }// End of init

    // ***** ActionPerformed method for ActionListener****/

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == record) {
            record.setEnabled(false);
            stop.setEnabled(true);
            play.setEnabled(false);
            send.setEnabled(false);
            recordAudio();
        } else if (e.getSource() == play) {
            if (file_chosed) {
                stop.setEnabled(false);
                send.setEnabled(false);
            } else {
                stop.setEnabled(true);
                send.setEnabled(false);
                if (first) {
                    playAudio();
                }
            }
        } else if (e.getSource() == stop) {
            if (file_chosed)
            {
            	
            }
            else {                
                stopAudio();
                saveAudio();
                play.setEnabled(true);

            }
            record.setEnabled(true);
            stop.setEnabled(false);            
            send.setEnabled(true);

        } else if (e.getSource() == send) {
        	
        	uploadState.setText("Envoi en cours, Veuillez patienter");  
        	progressBar.show();
            send.setEnabled(false);            
            try {
				UploadToServer();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

        } else if (e.getSource() == open_file) {

            choose_file();
            send.setEnabled(true);
            stop.setEnabled(false);
            play.setEnabled(false);
            record.setEnabled(true);
        }
    }
 
    // ************** Method Declarations ****/

    public void choose_file() {

        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Fichiers Audio.", "au", "mp3", "wav", "m4a", "mp4", "flac");
        chooser.setFileFilter(filter);
        chooser.setDialogTitle("envoyer un fichier audio préenregistré");
        int returnVal = chooser.showOpenDialog(getParent());
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            choosed_file.setText(chooser.getSelectedFile().getPath());
            file_chosed = true;
        }
        if(jso != null )
        {
            try {
                jso.call("changeFilePath", new String[] {chooser.getSelectedFile().getPath()});
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            
        }

    }

    public void recordAudio() {

        first = true;
        file_chosed = false;

        try {

            final AudioFileFormat.Type fileType = AudioFileFormat.Type.AU;
            final AudioFormat format = getFormat();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            Runnable runner = new Runnable() {
                int bufferSize = (int) format.getSampleRate()
                        * format.getFrameSize();
                byte buffer[] = new byte[bufferSize];

                public void run() {
                    out = new ByteArrayOutputStream();
                    running = true;

                    try {
                        while (running) {

                            int count = line.read(buffer, 0, buffer.length);
                            if (count > 0) {

                                out.write(buffer, 0, count);

                            }
                        }
                        out.close();
                    } catch (IOException e) {
                        System.exit(-1);
                    }
                }
            };
            Thread recordThread = new Thread(runner);
            recordThread.start();
        } catch (LineUnavailableException e) {
            System.err.println("Line Unavailable:" + e);
            e.printStackTrace();
            System.exit(-2);
        } catch (Exception e) {
            System.out.println("Direct Upload Error");
            e.printStackTrace();
        }

    }// End of RecordAudio method

    public void playAudio() {

        try {

            byte audio[] = out.toByteArray();
            InputStream input = new ByteArrayInputStream(audio);
            final AudioFormat format = getFormat();
            final AudioInputStream ais = new AudioInputStream(input, format,
                    audio.length / format.getFrameSize());
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            sline = (SourceDataLine) AudioSystem.getLine(info);
            sline.open(format);
            sline.start();
            Float audioLen = (audio.length / format.getFrameSize())
                    * format.getFrameRate();

            Runnable runner = new Runnable() {

                int bufferSize = (int) format.getSampleRate()
                        * format.getFrameSize();
                byte buffer[] = new byte[bufferSize];

                public void run() {

                    try {

                        int count;
                        synchronized (lock) {
                            while ((count = ais.read(buffer, 0, buffer.length)) != -1) {

                                while (paused) {

                                    if (sline.isRunning()) {

                                        sline.stop();
                                    }
                                    try {

                                        lock.wait();
                                    } catch (InterruptedException e) {
                                    }
                                }
                                if (!sline.isRunning()) {

                                    sline.start();
                                }
                                if (count > 0) {
                                    sline.write(buffer, 0, count);
                                }
                            }
                        }
                        first = true;
                        sline.drain();
                        sline.close();
                    } catch (IOException e) {
                        System.err.println("I/O problems:" + e);
                        System.exit(-3);
                    }
                }
            };

            Thread playThread = new Thread(runner);
            playThread.start();
        } catch (LineUnavailableException e) {
            System.exit(-4);
        }

    }// End of PlayAudio method

    public void stopAudio() {
    	running = false;
    	
        if (sline != null) {
            sline.stop();
            sline.close();
        } else {
            line.stop();
            line.close();
        }
    }// End of StopAudio

    public void UploadToServer() {
        	
    	Thread thread = new Upload();
        thread.start();

    }

    public void saveAudio() {

        Thread thread = new saveThread();
        thread.start();

    } // End of saveAudio

    private AudioFormat getFormat() {

        Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
        float sampleRate = 44100.0F;
        int sampleSizeInBits = 16;
        int channels = 2;
        int frameSize = 4;
        float frameRate = 44100.0F;
        boolean bigEndian = false;

        return new AudioFormat(encoding, sampleRate, sampleSizeInBits,
                channels, frameSize, frameRate, bigEndian);

    }// End of getAudioFormat

    class saveThread extends Thread {

        public void run() {

            AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
           // String name = "record" + ".wav";
            File file = new File(FileNameRecord);

            try {

                  byte audio[] = out.toByteArray();
                  InputStream input = new ByteArrayInputStream(audio);
                  final AudioFormat format = getFormat();
                  final AudioInputStream ais = new AudioInputStream(input,format, audio.length / format.getFrameSize());
                  AudioSystem.write(ais, fileType, file);

    

            } catch (Exception e) {
                e.printStackTrace();
            }


        }

    }
    
    class Upload extends Thread {

        public void run() {

        	File file = null;
            if (file_chosed)
            {
            	file = new File(choosed_file.getText());
            	System.out.println("le fichier choisi est perso ");
            }
            else
            {
            	file = new File(FileNameRecord);
            	System.out.println("le fichier choisi est record.wav ");
            }   
            if (!file.exists())
            {
            	try {
					throw new Exception("le fichier est introuvable !");
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            

            HttpClient httpclient = new DefaultHttpClient();
            httpclient.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

            HttpPost httppost = new HttpPost("http://pox.alwaysdata.net/other/tutorials/workclasslangue/model/student/upload.php?w="+work+"&u="+User);
            MultipartEntity mpEntity = new MultipartEntity();
            ContentBody cbFile = new FileBody(file, "audio/x-wav");
            mpEntity.addPart("userfile", cbFile);


            httppost.setEntity(mpEntity);
            System.out.println("executing request " + httppost.getRequestLine());
            if(jso != null )
            {
                try {
                    jso.call("changeUploadStat", new String[] {"executing request"});
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                
            }
            HttpResponse response = null;
			try {
				response = httpclient.execute(httppost);
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            HttpEntity resEntity = response.getEntity();

            System.out.println(response.getStatusLine());
            
            if(jso != null )
            {
                try {
                    jso.call("changeUploadStat", new String[] {response.getStatusLine().toString()});
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
                
            }
            
            
            if (resEntity != null) {
           	 try {
           		 String test = EntityUtils.toString(resEntity);
				System.out.println(test);
				if(jso != null )
	            {
	                try {
	                    jso.call("changeUploadStat", new String[] {test});
	                }
	                catch (Exception ex) {
	                    ex.printStackTrace();
	                }
	                
	            }
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
           	uploadState.setText("Envoyé avec succés");
           	
           	progressBar.hide();
            }
            if (resEntity != null) {
              try {
				resEntity.consumeContent();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            }

            httpclient.getConnectionManager().shutdown();
            
            File file2 = new File(FileNameRecord);
            if (file2.exists())
            {
            	file2.delete();
            }
            
        }

    }

    @Override
    public void itemStateChanged(ItemEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void stateChanged(ChangeEvent e) {
        // TODO Auto-generated method stub

    }

}// End of main