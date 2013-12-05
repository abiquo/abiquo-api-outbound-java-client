/**
 * The Abiquo Platform
 * Cloud management application for hybrid clouds
 * Copyright (C) 2008 - Abiquo Holdings S.L.
 *
 * This application is free software; you can redistribute it and/or
 * modify it under the terms of the GNU LESSER GENERAL PUBLIC
 * LICENSE as published by the Free Software Foundation under
 * version 3 of the License
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * LESSER GENERAL PUBLIC LICENSE v.3 for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package com.abiquo.bond.client;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

import com.abiquo.bond.api.ConfigurationData;
import com.abiquo.bond.api.OutboundAPIClient;
import com.abiquo.bond.api.plugin.PluginInterface;
import com.abiquo.bond.plugins.AllEvents;
import com.abiquo.bond.plugins.BackupEvents;

@SuppressWarnings("serial")
public class SimpleClient extends JFrame
{
    private static final SimpleDateFormat dateformat = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    private JTextField tfServer;

    private JTextField tfUser;

    private JPasswordField tfPassword;

    private JTextField tfStartDate;

    private OutboundAPIClient client;

    private JButton jButton1;

    private JButton jButton2;

    private JPanel jPanel1;

    private DateTextListener listener;

    public SimpleClient()
    {
        initComponents();
        BasicConfigurator.configure();
        File logprops = new File("logging.properties");
        PropertyConfigurator.configure(logprops.getAbsolutePath());
    }

    private void initComponents()
    {

        jPanel1 = new JPanel();
        jButton1 = new JButton();
        jButton2 = new JButton();

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        jButton1.setText("Connect");
        jButton1.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(final MouseEvent evt)
            {
                jButton1MouseClicked(evt);
            }
        });
        jPanel1.add(jButton1);

        jButton2.setText("Disconnect");
        jButton2.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(final MouseEvent evt)
            {
                jButton2MouseClicked(evt);
            }
        });
        jPanel1.add(jButton2);

        tfServer = new JTextField(10);
        tfUser = new JTextField(10);
        tfPassword = new JPasswordField(10);
        tfStartDate = new JTextField(10);
        tfStartDate.setText(dateformat.format(new Date()));
        listener = new DateTextListener();
        tfStartDate.getDocument().addDocumentListener(listener);

        // Create some labels for the fields.
        JLabel tflServer = new JLabel("Server: ");
        tflServer.setLabelFor(tfServer);
        JLabel tflUser = new JLabel("User: ");
        tflUser.setLabelFor(tfUser);
        JLabel tflPassword = new JLabel("Password: ");
        tflPassword.setLabelFor(tfPassword);
        JLabel tflStartDate = new JLabel("Last event date: ");
        tflStartDate.setLabelFor(tfStartDate);

        // Lay out the text controls and the labels.
        JPanel textControlsPane = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        textControlsPane.setLayout(gridbag);

        JLabel[] labels = {tflServer, tflUser, tflPassword, tflStartDate};
        JTextField[] textFields = {tfServer, tfUser, tfPassword, tfStartDate};
        addLabelTextRows(labels, textFields, gridbag, textControlsPane);

        c.gridwidth = GridBagConstraints.REMAINDER; // last
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1.0;

        getContentPane().add(textControlsPane, BorderLayout.NORTH);
        getContentPane().add(jPanel1, BorderLayout.SOUTH);

        pack();
    }

    private void jButton2MouseClicked(final MouseEvent evt)
    {
        client.close();
    }

    private void jButton1MouseClicked(final MouseEvent evt)
    {
        String server = tfServer.getText();
        String user = tfUser.getText();
        char[] pw = tfPassword.getPassword();

        try
        {
            ConfigurationData cfg = new ConfigurationData(server, user, new String(pw));
            if (listener.isChanged())
            {
                String date = tfStartDate.getText();
                if (date != null && date.length() > 0)
                {
                    try
                    {
                        cfg.setLastProcessedEvent(dateformat.parse(date));
                    }
                    catch (ParseException e)
                    {
                        System.out.println("Date conversion failed: " + e.getMessage());
                    }
                }
            }

            client = new OutboundAPIClient(cfg);
            List<Throwable> failures = client.getLoadFailures();
            if (failures.isEmpty())
            {
                NotificationHandler notifications = new NotificationHandler();
                client.setNotificationHandler(notifications);

                Set<PluginInterface> plugins = client.getPluginList();

                // Any configuration of the plugins should be done at this point
                for (PluginInterface plugin : plugins)
                {
                    // The sample plugins. The configure methods simply print out a message to say
                    // they have been called.
                    if (plugin instanceof AllEvents)
                    {
                        ((AllEvents) plugin).configure();
                    }
                    if (plugin instanceof BackupEvents)
                    {
                        ((BackupEvents) plugin).configure();
                    }
                }

                client.startPlugins();
                failures = client.getLoadFailures();
                if (failures.isEmpty())
                {
                    client.run();
                }
            }
            for (Throwable t : failures)
            {
                System.out.println("Plugin failure: " + t.getMessage());
            }
        }
        catch (Throwable e)
        {
            // TODO Auto-generated catch block
            JOptionPane.showMessageDialog(this, "Client startup failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(final String args[])
    {
        String classpath = System.getProperty("java.class.path");
        System.out.println("Classpath: " + classpath);
        System.out.println("Classpath contains open plugins jar: " + classpath.indexOf("plugins"));
        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                new SimpleClient().setVisible(true);
            }
        });
    }

    private void addLabelTextRows(final JLabel[] labels, final JTextField[] textFields,
        final GridBagLayout gridbag, final Container container)
    {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        int numLabels = labels.length;

        for (int i = 0; i < numLabels; i++)
        {
            c.gridwidth = GridBagConstraints.RELATIVE; // next-to-last
            c.fill = GridBagConstraints.NONE; // reset to default
            c.weightx = 0.0; // reset to default
            container.add(labels[i], c);

            c.gridwidth = GridBagConstraints.REMAINDER; // end row
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            container.add(textFields[i], c);
        }
    }
}

class DateTextListener implements DocumentListener
{
    private boolean textchanged = false;

    public boolean isChanged()
    {
        return textchanged;
    }

    @Override
    public void insertUpdate(final DocumentEvent e)
    {
        textchanged = true;
    }

    @Override
    public void removeUpdate(final DocumentEvent e)
    {
        textchanged = true;
    }

    @Override
    public void changedUpdate(final DocumentEvent e)
    {
        textchanged = true;
    }
}
