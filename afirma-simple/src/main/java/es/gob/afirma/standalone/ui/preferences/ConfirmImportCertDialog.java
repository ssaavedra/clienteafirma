/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 */

package es.gob.afirma.standalone.ui.preferences;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

import es.gob.afirma.core.misc.AOUtil;
import es.gob.afirma.core.misc.http.UrlHttpManagerImpl;
import es.gob.afirma.core.ui.AOUIFactory;
import es.gob.afirma.standalone.AutoFirmaUtil;
import es.gob.afirma.standalone.SimpleAfirmaMessages;
import es.gob.afirma.ui.core.jse.certificateselection.CertificateUtils;

final class ConfirmImportCertDialog extends JDialog  {

	private static final long serialVersionUID = -3168095095548385291L;

	private static final int PREFERRED_WIDTH = 600;
	private static final int PREFERRED_HEIGHT = 600;

	private final List<X509Certificate> certsToImport;
	private final KeyStore ks;
	private final boolean isLocalImport;

	private JButton openCertBtn;
	private JButton deleteCertBtn;
	private JButton importCertBtn;
	private JTable table;

	ConfirmImportCertDialog(final X509Certificate [] certsToImport, final KeyStore ks, final Container parent, final boolean isLocalImport) {
		this.certsToImport = new ArrayList<>(Arrays.asList(certsToImport));
		this.ks = ks;
		this.isLocalImport = isLocalImport;
	    createUI(parent);
	}

	void createUI(final Container parent) {

		setModal(true);
		setTitle(SimpleAfirmaMessages.getString("TrustedCertificatesDialog.6")); //$NON-NLS-1$
		setIconImages(AutoFirmaUtil.getIconImages());

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));

		setLayout(new GridBagLayout());

		final GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.gridy = 0;
		c.insets = new Insets(11,  11,  0,  11);

		final JLabel issuerDescLbl = new JLabel(SimpleAfirmaMessages.getString("TrustedCertificatesDialog.15")); //$NON-NLS-1$
		this.add(issuerDescLbl, c);

		c.weighty = 1.0;
		c.gridy++;
		c.insets = new Insets(5,  11,  0,  11);

		final JScrollPane certsScrollPane = createLoadedCertsTable();
		this.add(certsScrollPane, c);

		c.weighty = 0.0;
		c.gridy++;

		final JPanel certsButtonsPanel = createCertsButtonsPanel();
		this.add(certsButtonsPanel, c);

		c.gridy++;
		c.insets = new Insets(11,  11,  11,  11);

		final JPanel dialogButtonsPanel = createDialogButtonsPanel();
		this.add(dialogButtonsPanel, c);

        pack();
		setLocationRelativeTo(parent);
	}

	private JScrollPane createLoadedCertsTable() {

		final JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 11, 560, 227);

		final String[] columnNames = { "Nombre", "Emitido por", "Fecha de expiracion" };  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		final DefaultTableModel model = new DefaultTableModel(null, columnNames) {
			/** Serial Id. */
			private static final long serialVersionUID = -8796402986663262751L;

			@Override
			public boolean isCellEditable(final int row, final int column) {
				return false;
			}
		};

		final Object [] loadedCerts = new Object [this.certsToImport.size()];
		for (int i = 0 ; i < this.certsToImport.size(); i++) {
			final Object [] auxArray = {
					AOUtil.getCN(this.certsToImport.get(i).getSubjectX500Principal().toString()),
					AOUtil.getCN(this.certsToImport.get(i).getIssuerX500Principal().toString()),
					new SimpleDateFormat("dd-MM-yyyy").format(this.certsToImport.get(i).getNotAfter()).toString() //$NON-NLS-1$
			};
			loadedCerts[i] = auxArray;
		}

		for (int i = 0; i < loadedCerts.length ; i++) {
			model.addRow((Object[]) loadedCerts[i]);
		}

		this.table = new JTable(model);
		this.table.getSelectionModel().addListSelectionListener(event -> {
			ConfirmImportCertDialog.this.openCertBtn.setEnabled(true);
			ConfirmImportCertDialog.this.deleteCertBtn.setEnabled(true);
		});
		scrollPane.setViewportView(this.table);

		return scrollPane;
	}

	private JPanel createCertsButtonsPanel() {

		final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		this.openCertBtn = new JButton(SimpleAfirmaMessages.getString("TrustedCertificatesDialog.17")); //$NON-NLS-1$
		this.openCertBtn.addActionListener(
        		ae -> CertificateUtils.openCert(this, this.certsToImport.get(this.table.getSelectedRow()))
		);
		this.openCertBtn.setEnabled(false);
		buttonsPanel.add(this.openCertBtn);

		this.deleteCertBtn = new JButton(SimpleAfirmaMessages.getString("TrustedCertificatesDialog.18")); //$NON-NLS-1$
		this.deleteCertBtn.addActionListener(
        		ae -> {
        			final int idx = this.table.getSelectedRow();
        			((DefaultTableModel) this.table.getModel()).removeRow(idx);
        			this.certsToImport.remove(idx);

        			// Si no queda ningun elemento seleccionado, se deshabilitan los botones
        			// para abrir y omitir certificados
        			if (this.table.getSelectedRow() == -1) {
        				ConfirmImportCertDialog.this.openCertBtn.setEnabled(false);
        				ConfirmImportCertDialog.this.deleteCertBtn.setEnabled(false);
        			}

        			// Si no quedan certificados, se deshabilita el boton para importarlos
        			if (this.table.getRowCount() == 0) {
        				ConfirmImportCertDialog.this.importCertBtn.setEnabled(false);
        			}
        		}
		);
		this.deleteCertBtn.setEnabled(false);
		buttonsPanel.add(this.deleteCertBtn);

		return buttonsPanel;
	}

	private JPanel createDialogButtonsPanel() {

		final JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		this.importCertBtn = new JButton(SimpleAfirmaMessages.getString("TrustedCertificatesDialog.19")); //$NON-NLS-1$
		this.importCertBtn.addActionListener(
        		ae -> {
        			importCerts(this);
				}
		);
		buttonsPanel.add(this.importCertBtn);

		final JButton closeDialogButton = new JButton(SimpleAfirmaMessages.getString("TrustedCertificatesDialog.28")); //$NON-NLS-1$
		closeDialogButton.addActionListener(e -> dispose());
		buttonsPanel.add(closeDialogButton);

		return buttonsPanel;
	}

	private void importCerts(final Container parent) {
		final String trustedCertKSPath = ImportCertificatesDialog.getTrustedCertKSPath();

		try (final OutputStream fos = new FileOutputStream(trustedCertKSPath)) {
			if (this.certsToImport.size() == 1) {
				this.ks.setCertificateEntry(this.certsToImport.get(0).getSubjectDN().toString(), this.certsToImport.get(0));
			} else if (this.isLocalImport){
				for (int i = 0; i < this.certsToImport.size(); i++) {
					this.ks.setCertificateEntry(this.certsToImport.get(i).getSubjectDN().toString(), this.certsToImport.get(i));
				}
			} else {
				for (int i = 1; i < this.certsToImport.size(); i++) {
					this.ks.setCertificateEntry(this.certsToImport.get(i).getSubjectDN().toString(), this.certsToImport.get(i));
				}
			}
			this.ks.store(fos, ImportCertificatesDialog.TRUSTED_KS_PWD.toCharArray());
			UrlHttpManagerImpl.configureTrustManagers();
			setVisible(false);
		} catch (final Exception e) {
			AOUIFactory.showErrorMessage(
					parent,
					SimpleAfirmaMessages.getString("SignPanel.18"), //$NON-NLS-1$
					SimpleAfirmaMessages.getString("SimpleAfirma.48"), //$NON-NLS-1$
					JOptionPane.ERROR_MESSAGE,
					e);
		}
	 }
}