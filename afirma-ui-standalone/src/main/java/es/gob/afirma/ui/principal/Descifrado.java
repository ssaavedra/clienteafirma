/*
 * Este fichero forma parte del Cliente @firma.
 * El Cliente @firma es un applet de libre distribucion cuyo codigo fuente puede ser consultado
 * y descargado desde www.ctt.map.es.
 * Copyright 2009,2010 Ministerio de la Presidencia, Gobierno de Espana
 * Este fichero se distribuye bajo licencia GPL version 3 segun las
 * condiciones que figuran en el fichero 'licence' que se acompana.  Si se   distribuyera este
 * fichero individualmente, deben incluirse aqui las condiciones expresadas alli.
 */
package es.gob.afirma.ui.principal;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.Caret;

import es.gob.afirma.ui.listeners.ElementDescriptionFocusListener;
import es.gob.afirma.ui.listeners.ElementDescriptionMouseListener;
import es.gob.afirma.ui.utils.ConfigureCaret;
import es.gob.afirma.ui.utils.GeneralConfig;
import es.gob.afirma.ui.utils.HelpUtils;
import es.gob.afirma.ui.utils.JAccessibilityOptionPane;
import es.gob.afirma.ui.utils.Messages;
import es.gob.afirma.ui.utils.RequestFocusListener;
import es.gob.afirma.ui.utils.SelectionDialog;
import es.gob.afirma.ui.utils.Utils;
import es.gob.afirma.ui.wizarddescifradoclave.AsistenteDescifradoClave;
import es.gob.afirma.ui.wizarddescifradocontrasenia.AsistenteDescifradoContrasenia;

/**
 * Clase Descifrado que se encarga de descifrar un fichero cifrado.
 */
public class Descifrado extends JPanel {

	private static final long serialVersionUID = 1L;

    // Origen de la clave
    final static List<String> mecanismos = new ArrayList<String>(Arrays.asList("PASSWORD","USERINPUT"));

    // Algoritmos para mecanismo contrasena de cifrado
    final static List<String> algoritmoLc = new ArrayList<String>(Arrays.asList("Contraseña con SHA1 y 3DES","Contraseï¿½a con SHA1 y RC2","Contraseï¿½a con MD5 y DES"));
    final static List<String> algoritmoVc = new ArrayList<String>(Arrays.asList("PBEWithSHA1AndDESede","PBEWithSHA1AndRC2_40","PBEWithMD5AndDES"));

    // Algoritmos para mecanismo clave de cifrado
    final static List<String> algoritmoLr = new ArrayList<String>(Arrays.asList("Advanced Encryption Standard (AES)","Alleged RC4","Blowfish","Data Encryption Standard (DES)","Triple DES (3DES)","RC2"));
    final static List<String> algoritmoVr = new ArrayList<String>(Arrays.asList("AES","ARCFOUR","Blowfish","DES","DESede","RC2"));

    public Descifrado() {
        initComponents();
    }

    /**
     * Inicializacion de componentes
     */
    private void initComponents() {
		// Eliminamos el layout
		setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(13, 13, 0, 13);
		c.weightx = 1.0;
		c.gridwidth = 2;
		c.gridx = 0;
		
    	// Etiqueta fichero a descrifrar
        JLabel etiquetaFichero = new JLabel();
        etiquetaFichero.setText(Messages.getString("Descifrado.buscar")); // NOI18N
        Utils.setContrastColor(etiquetaFichero);
        Utils.setFontBold(etiquetaFichero);
		add(etiquetaFichero, c);
		
		c.insets = new Insets(0, 13, 0, 0);
		c.gridwidth = 1;
		c.gridy	= 1;

        // Caja con el nombre del archivo seleccionado
        final JTextField campoFichero = new JTextField();
        campoFichero.setToolTipText(Messages.getString("Descifrado.buscar.caja.description")); // NOI18N
        campoFichero.addMouseListener(new ElementDescriptionMouseListener(PrincipalGUI.bar, Messages.getString("Descifrado.buscar.caja.description")));
        campoFichero.addFocusListener(new ElementDescriptionFocusListener(PrincipalGUI.bar, Messages.getString("Descifrado.buscar.caja.description")));
        campoFichero.getAccessibleContext().setAccessibleName(etiquetaFichero.getText()+" ALT + E."); // NOI18N
		campoFichero.getAccessibleContext().setAccessibleDescription(Messages.getString("Cifrado.buscar.caja.description")); // NOI18N
		campoFichero.addAncestorListener(new RequestFocusListener(false));
     
        if (GeneralConfig.isBigCaret()) {
			Caret caret = new ConfigureCaret();
			campoFichero.setCaret(caret);
		}
        Utils.remarcar(campoFichero);
        Utils.setContrastColor(campoFichero);
        Utils.setFontBold(campoFichero);
		add(campoFichero, c);
		
		//Relación entre etiqueta y campo de texto
		etiquetaFichero.setLabelFor(campoFichero);
		//Asignación de mnemónico
		etiquetaFichero.setDisplayedMnemonic(KeyEvent.VK_E);
		
		c.insets = new Insets(0, 10, 0, 13);
		c.weightx = 0.0;
		c.gridx = 1;

		JPanel panelExaminar = new JPanel(new GridLayout(1, 1));
        // Boton examinar
        JButton examinar = new JButton();
        examinar.setMnemonic(KeyEvent.VK_X);
        examinar.setText(Messages.getString("PrincipalGUI.Examinar")); // NOI18N
        examinar.setToolTipText(Messages.getString("PrincipalGUI.Examinar.description")); // NOI18N
        examinar.addMouseListener(new ElementDescriptionMouseListener(PrincipalGUI.bar, Messages.getString("PrincipalGUI.Examinar.description")));
        examinar.addFocusListener(new ElementDescriptionFocusListener(PrincipalGUI.bar, Messages.getString("PrincipalGUI.Examinar.description")));
        examinar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                examinarActionPerformed(campoFichero);
            }
        });
        examinar.getAccessibleContext().setAccessibleName(Messages.getString("PrincipalGUI.Examinar") + " " + Messages.getString("PrincipalGUI.Examinar.description"));
		examinar.getAccessibleContext().setAccessibleDescription(Messages.getString("PrincipalGUI.Examinar.description")); // NOI18N
		
        Utils.remarcar(examinar);
        Utils.setContrastColor(examinar);
        Utils.setFontBold(examinar);
        panelExaminar.add(examinar);
		add(panelExaminar, c);
		
		c.insets = new Insets(13, 13, 0, 13);
		c.weightx = 1.0;
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy	= 2;
        
        // Etiqueta mecanismos de descifrado
        JLabel etiquetaMecanismo = new JLabel();
        etiquetaMecanismo.setText(Messages.getString("Descifrado.origen.clave")); // NOI18N
        Utils.setContrastColor(etiquetaMecanismo);
        Utils.setFontBold(etiquetaMecanismo);
        add(etiquetaMecanismo, c);

		c.insets = new Insets(0, 13, 0, 13);
		c.gridy = 3;
		c.weighty = 0.1;
		c.fill = GridBagConstraints.BOTH;
        
        // Combo mecanismos de cifrado
        final JComboBox comboMecanismo = new JComboBox();
        final JComboBox comboAlgoritmo = new JComboBox();
        comboMecanismo.setToolTipText(Messages.getString("Descifrado.origen.clave.combo.description")); // NOI18N

		comboMecanismo.getAccessibleContext().setAccessibleName(Messages.getString("Cifrado.origen.clave") +" "+ Messages.getString("Descifrado.origen.clave.combo.description")+" ALT + A."); // NOI18N
		comboMecanismo.getAccessibleContext().setAccessibleDescription(Messages.getString("Cifrado.origen.clave.combo.description")); // NOI18N
        comboMecanismo.addMouseListener(new ElementDescriptionMouseListener(PrincipalGUI.bar, Messages.getString("Descifrado.origen.clave.combo.description")));
        comboMecanismo.addFocusListener(new ElementDescriptionFocusListener(PrincipalGUI.bar, Messages.getString("Descifrado.origen.clave.combo.description")));
        comboMecanismo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
            	comboMecanismoItemStateChanged(comboMecanismo, comboAlgoritmo);
            }
        });
        comboMecanismo.setModel(new DefaultComboBoxModel(new String[]{Messages.getString("Descifrado.origenL.0"),Messages.getString("Descifrado.origenL.1")}));
        Utils.remarcar(comboMecanismo);
        Utils.setContrastColor(comboMecanismo);
        Utils.setFontBold(comboMecanismo);
        add(comboMecanismo, c);
        
        // En la vista simple, no se permitirá configurar el origen de la clave
 		if(!GeneralConfig.isAvanzados()) {
 			comboMecanismo.setEnabled(false); //Se deshabilita la opción
 			//Opciones para el lector de pantalla
			etiquetaMecanismo.setFocusable(true);
			etiquetaMecanismo.getAccessibleContext().setAccessibleName(etiquetaMecanismo.getText() + " " + Messages.getString("Cifrado.origen.clave.combo.defaultOpcion") 
			+ Messages.getString("Cifrado.origenL.0") + " "+ Messages.getString("Cifrado.origen.clave.combo.disabled"));
 		} else {
 			//Para la vista avanzada se asigna mnemónico puesto que esta opción estará habilitada
 			//Relación entre etiqueta y combo
 			etiquetaMecanismo.setLabelFor(comboMecanismo);
 			//Asignación de mnemónico
 			etiquetaMecanismo.setDisplayedMnemonic(KeyEvent.VK_A);
 		}
		
		c.insets = new Insets(13, 13, 0, 13);
		c.weightx = 1.0;
		c.gridy = 4;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.HORIZONTAL;
        
        // Etiqueta algoritmos de descifrado
        JLabel etiquetaAlgoritmo = new JLabel();
        etiquetaAlgoritmo.setText(Messages.getString("Descifrado.formato")); // NOI18N
        Utils.setContrastColor(etiquetaAlgoritmo);
        Utils.setFontBold(etiquetaAlgoritmo);
		add(etiquetaAlgoritmo, c);
		
		c.insets = new Insets(0, 13, 0, 13);
		c.gridy = 5;
		c.weighty = 0.1;
		c.fill = GridBagConstraints.BOTH;
        
        // Combo con los algoritmos de descifrado
        comboAlgoritmo.setModel(new DefaultComboBoxModel(new String[] { "Triple Data Encryption Standard (3DES)", "Item 2", "Item 3", "Item 4" }));
        comboAlgoritmo.setToolTipText(Messages.getString("Descifrado.formato.combo.description")); // NOI18N
        comboAlgoritmo.addMouseListener(new ElementDescriptionMouseListener(PrincipalGUI.bar, Messages.getString("Descifrado.formato.combo.description")));
        comboAlgoritmo.addFocusListener(new ElementDescriptionFocusListener(PrincipalGUI.bar, Messages.getString("Descifrado.formato.combo.description")));
        comboAlgoritmo.getAccessibleContext().setAccessibleName(etiquetaAlgoritmo.getText() +" "+ Messages.getString("Descifrado.formato.combo.description")+" ALT + G."); // NOI18N); // NOI18N
		comboAlgoritmo.getAccessibleContext().setAccessibleDescription(Messages.getString("Descifrado.formato.combo.description")); // NOI18N
        comboAlgoritmo.setModel(new DefaultComboBoxModel(algoritmoLc.toArray()));
        
        Utils.remarcar(comboAlgoritmo);
        Utils.setContrastColor(comboAlgoritmo);
        Utils.setFontBold(comboAlgoritmo);
		add(comboAlgoritmo, c);
		
		// En la vista simple, no se permitirá configurar el algoritmo de descifrado
		if(!GeneralConfig.isAvanzados()) {
			comboAlgoritmo.setEnabled(false); //Se deshabilita la opción
			//Opciones para el lector de pantalla
			etiquetaAlgoritmo.setFocusable(true);
			etiquetaAlgoritmo.getAccessibleContext().setAccessibleName(etiquetaAlgoritmo.getText() + " " + Messages.getString("Cifrado.origen.clave.combo.defaultOpcion") 
			+ Messages.getString("Cifrado.origenLc.0") + " " + Messages.getString("Cifrado.origen.clave.combo.disabled"));
		} else {
			//Para la vista avanzada se asigna mnemónico puesto que esta opción estará habilitada
			//Relación entre etiqueta y combo
			etiquetaAlgoritmo.setLabelFor(comboAlgoritmo);
			//Asignación de mnemónico
			etiquetaAlgoritmo.setDisplayedMnemonic(KeyEvent.VK_G);
		}
        
		c.weighty = 1.0;
		c.gridy = 6;
		c.fill = GridBagConstraints.HORIZONTAL;
		
		// Panel vacio para alinear el boton de aceptar en la parte de abajo de la pantalla
		JPanel emptyPanel = new JPanel();
		add(emptyPanel, c);
		
		// Panel con los botones
		JPanel panelBotones = new JPanel(new GridBagLayout());
		
		GridBagConstraints cons = new GridBagConstraints();
		cons.fill = GridBagConstraints.HORIZONTAL;
		cons.ipadx = 15;
		cons.gridx = 0;
		
		// Etiqueta para rellenar a la izquierda
		JLabel label = new JLabel();
		panelBotones.add(label, cons);
        
		JPanel panelDescifrar = new JPanel(new GridLayout(1, 1));
        // Boton descifrar
        JButton descifrar = new JButton();
        descifrar.setMnemonic(KeyEvent.VK_R);
        descifrar.setText(Messages.getString("Descifrado.btndescifrar")); // NOI18N
        descifrar.setToolTipText(Messages.getString("Descifrado.btndescifrar.description")); // NOI18N
        descifrar.setMaximumSize(null);
        descifrar.setMinimumSize(null);
        descifrar.setPreferredSize(null);
        descifrar.addMouseListener(new ElementDescriptionMouseListener(PrincipalGUI.bar, Messages.getString("Desensobrado.btnDescifrar.description")));
        descifrar.addFocusListener(new ElementDescriptionFocusListener(PrincipalGUI.bar, Messages.getString("Desensobrado.btnDescifrar.description")));
        descifrar.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                descifrarActionPerformed(comboMecanismo, comboAlgoritmo, campoFichero);
            }
        });
        descifrar.getAccessibleContext().setAccessibleName(descifrar.getText()+ " " + Messages.getString("Desensobrado.btnDescifrar.description")); // NOI18N
        descifrar.getAccessibleContext().setAccessibleDescription(Messages.getString("Desensobrado.btnDescifrar.description")); // NOI18N
        Utils.remarcar(descifrar);
        Utils.setContrastColor(descifrar);
        Utils.setFontBold(descifrar);
        
        panelDescifrar.add(descifrar);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(panelDescifrar, BorderLayout.CENTER);
		
		cons.ipadx = 0;
		cons.gridx = 1;
		cons.weightx = 1.0;
		
		panelBotones.add(buttonPanel, cons);
        
		JPanel panelAyuda = new JPanel(new GridLayout(1, 1));
		// Boton ayuda
		JButton botonAyuda = HelpUtils.helpButton("descifrado");
        
		cons.ipadx = 15;
		cons.weightx = 0.0;
		cons.gridx = 2;
		
		panelAyuda.add(botonAyuda);
		panelBotones.add(panelAyuda, cons);

		c.gridwidth	= 2;
        c.insets = new Insets(13,13,13,13);
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridy = 7;
		
		add(panelBotones, c);
        
        // Accesos rapidos al menu de ayuda
        HelpUtils.enableHelpKey(campoFichero, "descifrado.fichero");
        HelpUtils.enableHelpKey(examinar, "descifrado.fichero");
        HelpUtils.enableHelpKey(comboMecanismo, "descifrado.mecanismo");
        HelpUtils.enableHelpKey(comboAlgoritmo, "descifrado.algoritmo");
    }

	/**
     * Boton examinar pulsado: Se muestra una ventana para seleccionar un fichero.
     * El nombre del fichero seleccionado se guardara en el campo.
     * @param campoFichero	Campo donde se guarda el nombre del fichero seleccionado
     */
    private void examinarActionPerformed(JTextField campoFichero) {
    	File selectedFile = new SelectionDialog().showFileOpenDialog(this,Messages.getString("Seleccione.fichero.descifrar") );
    	if (selectedFile != null) 
    		campoFichero.setText(selectedFile.getAbsolutePath());
    }
    
    /**
	 * Cambio de seleccion en el combo de los mecanismos
	 * @param comboMecanismo	Combo que contiene el listado de mecanismos de cifrado
	 * @param comboAlgoritmo	Combo que contiene el listado de algoritmos
	 */
    private void comboMecanismoItemStateChanged(JComboBox comboMecanismo, JComboBox comboAlgoritmo) {
        String mecanismo = mecanismos.get(comboMecanismo.getSelectedIndex());
        if (mecanismo.equals("PASSWORD"))
        	comboAlgoritmo.setModel(new DefaultComboBoxModel(algoritmoLc.toArray()));
        else
        	comboAlgoritmo.setModel(new DefaultComboBoxModel(algoritmoLr.toArray()));
    }

    /**
	 * Pulsar boton descifrar: Descifra el archivo seleccionado con la configuracion seleccionada
	 * @param comboMecanismo 	Combo con el mecanismo de cifrado
	 * @param comboAlgoritmo	Combo con el algoritmo de cifrado
	 * @param campoFichero 		Campo con el nombre del fichero a cifrar
	 */
    private void descifrarActionPerformed(JComboBox comboMecanismo, JComboBox comboAlgoritmo, 
    		JTextField campoFichero) {
        String algoritmo = "";
        String mecanismo = mecanismos.get(comboMecanismo.getSelectedIndex());
        if (mecanismo.equals("PASSWORD"))
            algoritmo = algoritmoVc.get(comboAlgoritmo.getSelectedIndex());
        else
            algoritmo = algoritmoVr.get(comboAlgoritmo.getSelectedIndex());
        
        // Sacamos la ruta del archivo
        if (campoFichero.getText() == null || campoFichero.getText().equals("")) {
			JAccessibilityOptionPane.showMessageDialog(this, Messages.getString("Descifrado.msg.error.fichero"), Messages.getString("Descifrado.msg.titulo"), JOptionPane.WARNING_MESSAGE);
        	campoFichero.requestFocusInWindow(); //Foco al campo que contiene el path al fichero
        }
        else
        	// Se selecciona el primer elemento del combo
            if (mecanismo.equals("PASSWORD")) 
                // Se muestra el asistente de descifrado con contrasena
            	new AsistenteDescifradoContrasenia(algoritmo,campoFichero.getText());
            else 
                // Se muestra el asistente de descifrado con clave
                new AsistenteDescifradoClave(algoritmo,campoFichero.getText());
    }
}
