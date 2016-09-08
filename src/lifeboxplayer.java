
import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class lifeboxplayer extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;
    protected JTextField TFDropboxPath;
    protected JTextField TFLocalPath;
    protected JTextField TFVlcPath;
    protected JPasswordField PFPassword;
    protected JButton JBStart = new JButton("Start");
    protected JTextArea TAStatusLog;
    protected JScrollPane sp;
    
    private static String homeDir = System.getProperty("user.home");

    public static void main(String[] args) {
        
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    public lifeboxplayer() {
        super(new GridBagLayout());

        TFDropboxPath = new JTextField(100);
        TFLocalPath = new JTextField(100);
        TFVlcPath = new JTextField(100);
        PFPassword = new JPasswordField(100);
        TAStatusLog = new JTextArea(10,100);
        sp = new JScrollPane(TAStatusLog); 

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; c.gridwidth = 1;
        add(new JLabel("Dropbox directory", JLabel.RIGHT), c);

        c.gridx = 1; c.gridy = 0; c.gridwidth = 2;
        add(TFDropboxPath, c);

        c.gridx = 0; c.gridy = 1; c.gridwidth = 1;
        add(new JLabel("Local directory", JLabel.RIGHT), c);

        c.gridx = 1; c.gridy = 1; c.gridwidth = 2;
        add(TFLocalPath, c);

        c.gridx = 0; c.gridy = 2; c.gridwidth = 1;
        add(new JLabel("Path to VLC", JLabel.RIGHT), c);

        c.gridx = 1; c.gridy = 2; c.gridwidth = 2;
        add(TFVlcPath, c);

        c.gridx = 0; c.gridy = 3; c.gridwidth = 1;
        add(new JLabel("Encryption Password", JLabel.RIGHT), c);

        c.gridx = 1; c.gridy = 3; c.gridwidth = 2;
        add(PFPassword, c);

        c.gridx = 1; c.gridy = 4; c.gridwidth = 1;
        add(JBStart, c);

        c.gridx = 1; c.gridy = 5; c.gridwidth = 1;
        add(sp, c);
        
        Path dropboxPath = FileSystems.getDefault().getPath(homeDir)
                .resolve("Dropbox").resolve("Apps").resolve("LifeBoxApp");
        TFDropboxPath.setText(dropboxPath.toString());
        
        Path localPath = FileSystems.getDefault().getPath(homeDir)
                .resolve("Documents").resolve("LifeBoxApp");
        TFLocalPath.setText(localPath.toString());
        
        String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((OS.contains("mac")) || (OS.contains("darwin"))) {
            //MacOS
            TFVlcPath.setText("/Applications/VLC.app/Contents/MacOS/VLC");
        } else if (OS.contains("win")) {
            //Windows
            TFVlcPath.setText("C:\\Program Files (x86)\\VideoLAN\\VLC\\vlc.exe");
        } else if (OS.contains("nux")) {
            //Linux
            TFVlcPath.setText("/usr/bin/vlc");
        } else {
            //Other?
        }
        
        JBStart.setActionCommand("DoTheThing");
        JBStart.addActionListener(this);
    }

    public void actionPerformed(ActionEvent evt) {
        File dropboxPath = new File(TFDropboxPath.getText());
        File localPath = new File(TFLocalPath.getText());
        
        boolean ready = true;
        
        if (dropboxPath.isDirectory()) {
            TFDropboxPath.setBackground(Color.green);
            int fileCount = 0;
            for (File child : dropboxPath.listFiles()) {
                if (".".equals(child.getName()) || "..".equals(child.getName()))
                    continue;
                if (child.getName().contains("_") && 
                        (child.getName().endsWith(".3gp") || 
                        child.getName().endsWith(".mp4") || 
                        child.getName().endsWith(".enc")))  {
                    fileCount+=1;
                }
            }
            TAStatusLog.append("Counted " + fileCount + " files in Dropbox directory.\n");
        }
        else {
            TFDropboxPath.setBackground(Color.red);
            TAStatusLog.append("Dropbox directory does not exist.\n");
            ready = false;
        }
        
        if (localPath.isDirectory()) {
            TFLocalPath.setBackground(Color.green);
            int fileCount = 0;
            for (File child : localPath.listFiles()) {
                if (".".equals(child.getName()) || "..".equals(child.getName()))
                    continue;
                if (child.getName().contains("_") && 
                        (child.getName().endsWith(".3gp") || 
                        child.getName().endsWith(".mp4")))  {
                    fileCount+=1;
                }
            }
            TAStatusLog.append("Counted " + fileCount + " files found in local directory.\n");
        }
        else {
            TAStatusLog.append("Local directory does not exist, attempting to create it.\n");
            boolean makeLocalDir = localPath.mkdir();
            if (makeLocalDir) {
                TAStatusLog.append("Created new local directory.\n");
                TFLocalPath.setBackground(Color.green);
            }
            else {
                ready = false;
                TFLocalPath.setBackground(Color.red);
            }
        }
        File vlcPath = new File(TFVlcPath.getText());
        if (vlcPath.isFile()) {
            TFVlcPath.setBackground(Color.green);
        }
        else {
            TFVlcPath.setBackground(Color.red);
            TAStatusLog.append("VLC not found.\n");
            //ready = false;
        }
        
        if (ready) {
            TAStatusLog.append("Ready to do stuff!\n");
        }
        
        int unencrypted = 0;
        int encryptedSuccess = 0;
        int encryptedFail = 0;
        for (File file : dropboxPath.listFiles()) {
            if (".".equals(file.getName()) || "..".equals(file.getName()))
                continue;
            if (file.getName().contains("_") && 
                    (file.getName().endsWith(".3gp") || 
                    file.getName().endsWith(".mp4")))  {
                unencrypted += 1;
                String newName = new File(localPath,file.getName()).toString();
                file.renameTo(new File(newName));
            }
            else if (file.getName().contains("_") && file.getName().endsWith(".enc")) {
                String fileName = file.getName();
                String deviceID = fileName.split("_")[0];
                String password = new String(PFPassword.getPassword());
                if (password.isEmpty()) {
                    encryptedFail += 1;
                }
                else
                {
                    try {
                        FileInputStream fis = new FileInputStream(file);
                        String newName;
                        if (fileName.contains(".3gp"))
                            newName = new File(localPath,file.getName()
                                .split("\\.")[0]+".3gp").toString();
                        else
                            newName = new File(localPath,file.getName()
                                .split("\\.")[0]+".mp4").toString();
                        int fileLength = fis.available();
                        byte[] fileBytes = new byte[fileLength];
                        fis.read(fileBytes);
                        byte[] decrypted = decryptFile(password,deviceID,fileBytes);
                        FileOutputStream fos = new FileOutputStream(newName);
                        fos.write(decrypted);
                        fos.close();
                        fis.close();
                        encryptedSuccess += 1;
                        
                        if (!file.delete())
                            System.out.println("Failed to delete "+file.getName());
                    }
                    catch (Exception e) {
                        encryptedFail += 1;
                    }
                }
            }
        }
        
        TAStatusLog.append("Moved "+unencrypted+" unencrypted files from Dropbox to local.\n");
        TAStatusLog.append("Successfully decrypted "+encryptedSuccess+" files.\n");
        TAStatusLog.append("Failed to decrypt "+encryptedFail+" files.\n");
        
        /*for (File file : localPath.listFiles()) {
            if (".".equals(file.getName()) || "..".equals(file.getName()))
                continue;
            if (file.getName().contains("_") && file.getName().endsWith(".3gp")) {
                try {
                    String fileName = file.getName();
                    String deviceID = fileName.split("_")[0];
                    String timeStamp = fileName.split("_")[1].split("\\.")[0];
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss");
                    // Timestamp in 3GP filenames are in local time.
                    Date fileDate = formatter.parse(timeStamp);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
            if (file.getName().contains("_") && file.getName().endsWith(".mp4")) {
                try {
                    String fileName = file.getName();
                    String deviceID = fileName.split("_")[0];
                    String timeStamp = fileName.split("_")[1].split("\\.")[0];
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
                    // Timestamp in MP4 filenames are in UTC time.
                    formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                    Date fileDate = formatter.parse(timeStamp);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }*/
        
        TAStatusLog.append("Finished.\n");
        
        try {
            Desktop.getDesktop().open(new File(TFLocalPath.getText()));
        } catch (Exception ex) {
            TAStatusLog.append(ex.getMessage());
        }
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("LifeBoxPlayer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new lifeboxplayer());
        frame.pack();
        frame.setVisible(true);
    }

    private static byte[] decryptFile(String password, String salt, byte[] encrypted) throws Exception {
        String encryptionAlgorithm = "AES";
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes("UTF-8"), 1000, 128);
        SecretKeyFactory skf;
        skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] keyBytes = skf.generateSecret(spec).getEncoded();
        SecretKeySpec keySpecification = new SecretKeySpec(keyBytes, encryptionAlgorithm);
        Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
        cipher.init(Cipher.DECRYPT_MODE, keySpecification);
        return cipher.doFinal(encrypted);
    }
}
