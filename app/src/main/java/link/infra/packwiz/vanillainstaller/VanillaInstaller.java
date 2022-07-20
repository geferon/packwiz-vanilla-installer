package link.infra.packwiz.vanillainstaller;

import link.infra.packwiz.vanillainstaller.meta.*;
import link.infra.packwiz.vanillainstaller.util.Debouncer;
import link.infra.packwiz.vanillainstaller.util.PathUtils;
import link.infra.packwiz.vanillainstaller.util.Tuple;
import net.fabricmc.installer.Main;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class VanillaInstaller {
	private static final String[] VALID_URL_SCHEMES = {"http", "https", "file", "github"};
	private static final ArrayList<Tuple<String, Class>> LOADERS = new ArrayList<>(){
			{
					add(new Tuple<>("fabric", FabricMetadata.class));
					add(new Tuple<>("quilt", QuiltMetadata.class));
					add(new Tuple<>("forge", ForgeMetadata.class));
			}
	};

	private final UrlValidator urlValidator = new UrlValidator(VALID_URL_SCHEMES);
	private final PackMetadataRetriever metadataRetriever = new PackMetadataRetriever() {
		@Override
		public void onDataUpdated(Data data) {
			onPackDataLoaded(data);
		}
	};
	private LoaderMetadataGetter loaderMetaGetter = null;

	// GUI vars
	private JFrame mainFrame;
	private JTextField packUrlField;
	private JLabel warningLabel;
	private JPanel advancedOptionsPanel;
	private JCheckBox checkboxInstallLoader;
	private JButton refreshVersionsButton;
	private JTextField createdVersionNameField;
	private JButton launcherPathBrowseButton;
	private JButton gameDirectoryUseMcFolderButton;
	private JButton gameDirectoryGeneratedPathButton;
	private JButton gameDirectoryBrowseButton;
	private JTextField gameDirectoryField;
	private ButtonGroup loaderGroup;
	private JComboBox<String> minecraftVersionComboBox;
	private JComboBox<String> loaderVersionComboBox;
	private JTextField installationNameField;
	private JButton nextButton;
	private JTextField launcherPathField;
	private JProgressBar loadingBar;

	private HashMap<String, JRadioButton> loaderRadioButtons = new HashMap<>();
	private JRadioButton radioButtonLoaderOther;

	private final Debouncer urlDebouncer = new Debouncer(() -> {
		downloadPackFile(packUrlField.getText());
	}, 4000);

	public static void main(String[] args) {
		EventQueue.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				VanillaInstaller window = new VanillaInstaller();
				window.mainFrame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public VanillaInstaller() {
		initialize();
		validate();
	}

	private void initialize() {
		mainFrame = new JFrame();
		mainFrame.setTitle("packwiz Vanilla Launcher installer");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.getContentPane().setLayout(new GridBagLayout());

		JPanel formContainer = new JPanel();
		GridBagConstraints gbc_formLayout = new GridBagConstraints();
		gbc_formLayout.fill = GridBagConstraints.BOTH;
		gbc_formLayout.insets = new Insets(0, 0, 5, 5);
		gbc_formLayout.gridx = 0;
		gbc_formLayout.gridy = 0;
		mainFrame.getContentPane().add(formContainer, gbc_formLayout);
		formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));

		JLabel titleLabel = new JLabel("This installer creates an installation in the official Minecraft Launcher for a packwiz modpack.");
		titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
		titleLabel.setBorder(new EmptyBorder(5, 5, 5, 5));
		titleLabel.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
		formContainer.add(titleLabel);

		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		formContainer.add(panel);
		GridBagLayout gbl_panel = new GridBagLayout();
		gbl_panel.columnWidths = new int[]{0, 0, 0, 0};
		gbl_panel.rowHeights = new int[]{0, 0};
		gbl_panel.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_panel.rowWeights = new double[]{0.0, 0.0};
		panel.setLayout(gbl_panel);

		JLabel packUrlLabel = new JLabel("Pack URL");
		packUrlLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_packUrlLabel = new GridBagConstraints();
		gbc_packUrlLabel.fill = GridBagConstraints.HORIZONTAL;
		gbc_packUrlLabel.insets = new Insets(0, 0, 5, 5);
		gbc_packUrlLabel.gridx = 0;
		gbc_packUrlLabel.gridy = 0;
		panel.add(packUrlLabel, gbc_packUrlLabel);

		packUrlField = new JTextField();
		GridBagConstraints gbc_packUrlField = new GridBagConstraints();
		gbc_packUrlField.insets = new Insets(0, 0, 5, 5);
		gbc_packUrlField.fill = GridBagConstraints.HORIZONTAL;
		gbc_packUrlField.gridx = 1;
		gbc_packUrlField.gridy = 0;
		panel.add(packUrlField, gbc_packUrlField);
		packUrlField.setColumns(10);

		JLabel installationNameLabel = new JLabel("Installation name");
		installationNameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_installationNameLabel = new GridBagConstraints();
		gbc_installationNameLabel.fill = GridBagConstraints.HORIZONTAL;
		gbc_installationNameLabel.insets = new Insets(0, 0, 5, 5);
		gbc_installationNameLabel.gridx = 0;
		gbc_installationNameLabel.gridy = 1;
		panel.add(installationNameLabel, gbc_installationNameLabel);

		installationNameField = new JTextField();
		GridBagConstraints gbc_installationNameField = new GridBagConstraints();
		gbc_installationNameField.insets = new Insets(0, 0, 5, 5);
		gbc_installationNameField.fill = GridBagConstraints.HORIZONTAL;
		gbc_installationNameField.gridx = 1;
		gbc_installationNameField.gridy = 1;
		panel.add(installationNameField, gbc_installationNameField);
		installationNameField.setColumns(10);


		// Bottom bar
		JPanel bottomBarContainer = new JPanel();
		GridBagConstraints gbc_bottomBarLayout = new GridBagConstraints();
		gbc_bottomBarLayout.fill = GridBagConstraints.HORIZONTAL;
		gbc_bottomBarLayout.insets = new Insets(0, 0, 5, 5);
		gbc_bottomBarLayout.gridx = 0;
		gbc_bottomBarLayout.gridy = 1;
		mainFrame.getContentPane().add(bottomBarContainer, gbc_bottomBarLayout);
		bottomBarContainer.setLayout(new BorderLayout(0, 0));

		JPanel nextButtonContainer = new JPanel();
		FlowLayout fl_nextButtonContainer = (FlowLayout) nextButtonContainer.getLayout();
		fl_nextButtonContainer.setAlignment(FlowLayout.RIGHT);
		bottomBarContainer.add(nextButtonContainer, BorderLayout.EAST);

		nextButton = new JButton("Next");
		nextButtonContainer.add(nextButton);

		JPanel warningLabelContainer = new JPanel();
		bottomBarContainer.add(warningLabelContainer, BorderLayout.CENTER);
		//warningLabelContainer.setLayout(new BorderLayout(0, 0));
		GridBagLayout gbl_warningLabel = new GridBagLayout();
		gbl_warningLabel.columnWidths = new int[]{1};
		gbl_warningLabel.rowHeights = new int[]{1};
		gbl_warningLabel.columnWeights = new double[]{1.0};
		gbl_warningLabel.rowWeights = new double[]{1.0};
		warningLabelContainer.setLayout(gbl_warningLabel);

		warningLabel = new JLabel("Pack URL is required");
		warningLabel.setHorizontalAlignment(SwingConstants.CENTER);
		warningLabel.setForeground(new Color(204, 0, 51));
		GridBagConstraints gbc_warningLabel = new GridBagConstraints();
		gbc_warningLabel.fill = GridBagConstraints.HORIZONTAL;
		gbc_warningLabel.insets = new Insets(5, 5, 5, 5);
		gbc_warningLabel.gridx = 0;
		gbc_warningLabel.gridy = 0;
		warningLabelContainer.add(warningLabel, gbc_warningLabel);

		loadingBar = new JProgressBar();
		loadingBar.setIndeterminate(true);
		loadingBar.setVisible(false);
		warningLabelContainer.add(loadingBar, gbc_warningLabel);

		JPanel bottomLeftContainer = new JPanel();
		bottomBarContainer.add(bottomLeftContainer, BorderLayout.WEST);

		JButton expandButton = new JButton("More options");
		expandButton.addActionListener(e -> {
			advancedOptionsPanel.setVisible(!advancedOptionsPanel.isVisible());
			expandButton.setText((advancedOptionsPanel.isVisible() ? "Less" : "More") + " options");
			mainFrame.pack();
		});
		bottomLeftContainer.add(expandButton);

		//JButton exitButton = new JButton("Exit");
		//exitButton.addActionListener(e -> mainFrame.dispose());
		//bottomLeftContainer.add(exitButton);



		// Advanced options

		advancedOptionsPanel = new JPanel();
		advancedOptionsPanel.setVisible(false);
		GridBagConstraints gbc_panel_2 = new GridBagConstraints();
		gbc_panel_2.fill = GridBagConstraints.HORIZONTAL;
		gbc_panel_2.gridx = 0;
		gbc_panel_2.gridy = 2;
		//panel.add(advancedOptionsPanel, gbc_panel_2);
		mainFrame.getContentPane().add(advancedOptionsPanel, gbc_panel_2);
		advancedOptionsPanel.setBorder(new TitledBorder(null, "Advanced options", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		GridBagLayout gbl_panel_2 = new GridBagLayout();
		gbl_panel_2.columnWidths = new int[]{0, 0, 0, 0};
		//gbl_panel_2.rowHeights = new int[]{0, 0, 0};
		gbl_panel_2.rowHeights = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		gbl_panel_2.columnWeights = new double[]{0.0, 1.0, 0.0, Double.MIN_VALUE};
		gbl_panel_2.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
		advancedOptionsPanel.setLayout(gbl_panel_2);

		// MC version / loader

		JLabel modLoaderLabel = new JLabel("Mod loader");
		modLoaderLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_modLoaderLabel = new GridBagConstraints();
		gbc_modLoaderLabel.fill = GridBagConstraints.HORIZONTAL;
		gbc_modLoaderLabel.insets = new Insets(0, 0, 5, 5);
		gbc_modLoaderLabel.gridx = 0;
		gbc_modLoaderLabel.gridy = 0;
		advancedOptionsPanel.add(modLoaderLabel, gbc_modLoaderLabel);

		JPanel panel_3 = new JPanel();
		GridBagConstraints gbc_panel_3 = new GridBagConstraints();
		gbc_panel_3.insets = new Insets(0, 0, 5, 5);
		gbc_panel_3.gridx = 1;
		gbc_panel_3.gridy = 0;
		advancedOptionsPanel.add(panel_3, gbc_panel_3);
		panel_3.setLayout(new BoxLayout(panel_3, BoxLayout.X_AXIS));

		loaderGroup = new ButtonGroup();

		for (var loader : LOADERS) {
			var button = new JRadioButton(StringUtils.capitalize(loader.x));
			button.putClientProperty("MCLoaderClass", loader.y);
			loaderGroup.add(button);
			loaderRadioButtons.put(loader.x, button);
			panel_3.add(button);
		}

		radioButtonLoaderOther = new JRadioButton("Other");
		loaderGroup.add(radioButtonLoaderOther);
		panel_3.add(radioButtonLoaderOther);

		JLabel minecraftVersionLabel = new JLabel("Minecraft version");
		minecraftVersionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_minecraftVersionLabel = new GridBagConstraints();
		gbc_minecraftVersionLabel.anchor = GridBagConstraints.EAST;
		gbc_minecraftVersionLabel.insets = new Insets(0, 0, 5, 5);
		gbc_minecraftVersionLabel.gridx = 0;
		gbc_minecraftVersionLabel.gridy = 1;
		advancedOptionsPanel.add(minecraftVersionLabel, gbc_minecraftVersionLabel);

		minecraftVersionComboBox = new JComboBox<>();
		minecraftVersionComboBox.setEnabled(false); // Disabled by default until loaded
		GridBagConstraints gbc_minecraftVersionComboBox = new GridBagConstraints();
		gbc_minecraftVersionComboBox.insets = new Insets(0, 0, 5, 5);
		gbc_minecraftVersionComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_minecraftVersionComboBox.gridx = 1;
		gbc_minecraftVersionComboBox.gridy = 1;
		advancedOptionsPanel.add(minecraftVersionComboBox, gbc_minecraftVersionComboBox);

		refreshVersionsButton = new JButton("Refresh");
		GridBagConstraints gbc_refreshVersionsButton = new GridBagConstraints();
		gbc_refreshVersionsButton.insets = new Insets(0, 0, 5, 0);
		gbc_refreshVersionsButton.gridx = 2;
		gbc_refreshVersionsButton.gridy = 1;
		advancedOptionsPanel.add(refreshVersionsButton, gbc_refreshVersionsButton);

		checkboxInstallLoader = new JCheckBox("Install loader");
		checkboxInstallLoader.setSelected(true);
		GridBagConstraints gbc_checkboxInstallLoader = new GridBagConstraints();
		gbc_checkboxInstallLoader.insets = new Insets(0, 0, 5, 0);
		gbc_checkboxInstallLoader.gridx = 2;
		gbc_checkboxInstallLoader.gridy = 2;
		advancedOptionsPanel.add(checkboxInstallLoader, gbc_checkboxInstallLoader);

		JLabel loaderVersionLabel = new JLabel("Mod Loader version");
		loaderVersionLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_loaderVersionLabel = new GridBagConstraints();
		gbc_loaderVersionLabel.anchor = GridBagConstraints.EAST;
		gbc_loaderVersionLabel.insets = new Insets(0, 0, 5, 5);
		gbc_loaderVersionLabel.gridx = 0;
		gbc_loaderVersionLabel.gridy = 2;
		advancedOptionsPanel.add(loaderVersionLabel, gbc_loaderVersionLabel);

		loaderVersionComboBox = new JComboBox<>();
		loaderVersionComboBox.setEnabled(false); // Disabled by default until loaded
		GridBagConstraints gbc_loaderVersionComboBox = new GridBagConstraints();
		gbc_loaderVersionComboBox.insets = new Insets(0, 0, 5, 5);
		gbc_loaderVersionComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbc_loaderVersionComboBox.gridx = 1;
		gbc_loaderVersionComboBox.gridy = 2;
		advancedOptionsPanel.add(loaderVersionComboBox, gbc_loaderVersionComboBox);

		JLabel refreshHintLabel = new JLabel("Can't see the version you want? Try running it in the launcher first, then hit refresh.");
		GridBagConstraints gbc_refreshHintLabel = new GridBagConstraints();
		gbc_refreshHintLabel.gridwidth = 3;
		gbc_refreshHintLabel.insets = new Insets(0, 0, 5, 0);
		gbc_refreshHintLabel.gridx = 0;
		gbc_refreshHintLabel.gridy = 3;
		advancedOptionsPanel.add(refreshHintLabel, gbc_refreshHintLabel);

		Component glue = Box.createGlue();
		GridBagConstraints gbc_glue = new GridBagConstraints();
		gbc_glue.fill = GridBagConstraints.VERTICAL;
		gbc_glue.insets = new Insets(0, 0, 5, 5);
		gbc_glue.gridx = 1;
		gbc_glue.gridy = 4;
		advancedOptionsPanel.add(glue, gbc_glue);


		JSeparator separator_1 = new JSeparator();
		GridBagConstraints gbc_separator_1 = new GridBagConstraints();
		gbc_separator_1.fill = GridBagConstraints.BOTH;
		gbc_separator_1.gridwidth = 3;
		gbc_separator_1.insets = new Insets(0, 5, 5, 5);
		gbc_separator_1.gridx = 0;
		gbc_separator_1.gridy = 5;
		advancedOptionsPanel.add(separator_1, gbc_separator_1);

		// Game dir

		JLabel gameDirectoryLabel = new JLabel("Game directory");
		gameDirectoryLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		GridBagConstraints gbc_gameDirectoryLabel = new GridBagConstraints();
		gbc_gameDirectoryLabel.fill = GridBagConstraints.HORIZONTAL;
		gbc_gameDirectoryLabel.insets = new Insets(0, 0, 5, 5);
		gbc_gameDirectoryLabel.gridx = 0;
		gbc_gameDirectoryLabel.gridy = 6;
		advancedOptionsPanel.add(gameDirectoryLabel, gbc_gameDirectoryLabel);

		gameDirectoryField = new JTextField();
		GridBagConstraints gbc_gameDirectoryField = new GridBagConstraints();
		gbc_gameDirectoryField.insets = new Insets(0, 0, 5, 5);
		gbc_gameDirectoryField.fill = GridBagConstraints.HORIZONTAL;
		gbc_gameDirectoryField.gridx = 1;
		gbc_gameDirectoryField.gridy = 6;
		advancedOptionsPanel.add(gameDirectoryField, gbc_gameDirectoryField);
		gameDirectoryField.setColumns(10);

		gameDirectoryBrowseButton = new JButton("Browse...");
		GridBagConstraints gbc_gameDirectoryBrowseButton = new GridBagConstraints();
		gbc_gameDirectoryBrowseButton.insets = new Insets(0, 0, 5, 0);
		gbc_gameDirectoryBrowseButton.gridx = 2;
		gbc_gameDirectoryBrowseButton.gridy = 6;
		advancedOptionsPanel.add(gameDirectoryBrowseButton, gbc_gameDirectoryBrowseButton);

		JPanel panel_1 = new JPanel();
		GridBagConstraints gbc_panel_1 = new GridBagConstraints();
		gbc_panel_1.gridwidth = 3;
		gbc_panel_1.insets = new Insets(0, 0, 5, 0);
		gbc_panel_1.fill = GridBagConstraints.BOTH;
		gbc_panel_1.gridx = 0;
		gbc_panel_1.gridy = 7;
		advancedOptionsPanel.add(panel_1, gbc_panel_1);
		GridBagLayout gbl_panel_1 = new GridBagLayout();
		gbl_panel_1.columnWidths = new int[]{0, 0, 10, 0, 0, 0};
		gbl_panel_1.rowHeights = new int[]{0, 0};
		gbl_panel_1.columnWeights = new double[]{1.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE};
		gbl_panel_1.rowWeights = new double[]{0.0, Double.MIN_VALUE};
		panel_1.setLayout(gbl_panel_1);

		gameDirectoryGeneratedPathButton = new JButton("Use generated path");
		GridBagConstraints gbc_gameDirectoryGeneratedPathButton = new GridBagConstraints();
		gbc_gameDirectoryGeneratedPathButton.insets = new Insets(0, 0, 0, 5);
		gbc_gameDirectoryGeneratedPathButton.gridx = 1;
		gbc_gameDirectoryGeneratedPathButton.gridy = 0;
		panel_1.add(gameDirectoryGeneratedPathButton, gbc_gameDirectoryGeneratedPathButton);

		gameDirectoryUseMcFolderButton = new JButton("Use .minecraft");
		GridBagConstraints gbc_gameDirectoryUseMcFolderButton = new GridBagConstraints();
		gbc_gameDirectoryUseMcFolderButton.insets = new Insets(0, 0, 0, 5);
		gbc_gameDirectoryUseMcFolderButton.gridx = 3;
		gbc_gameDirectoryUseMcFolderButton.gridy = 0;
		panel_1.add(gameDirectoryUseMcFolderButton, gbc_gameDirectoryUseMcFolderButton);

		// Launcher path / crated version name

		JLabel launcherPathLabel = new JLabel("Launcher path");
		GridBagConstraints gbc_launcherPathLabel = new GridBagConstraints();
		gbc_launcherPathLabel.insets = new Insets(0, 0, 5, 5);
		gbc_launcherPathLabel.anchor = GridBagConstraints.EAST;
		gbc_launcherPathLabel.gridx = 0;
		gbc_launcherPathLabel.gridy = 8;
		advancedOptionsPanel.add(launcherPathLabel, gbc_launcherPathLabel);

		launcherPathField = new JTextField();
		GridBagConstraints gbc_launcherPathField = new GridBagConstraints();
		gbc_launcherPathField.insets = new Insets(0, 0, 5, 5);
		gbc_launcherPathField.fill = GridBagConstraints.HORIZONTAL;
		gbc_launcherPathField.gridx = 1;
		gbc_launcherPathField.gridy = 8;
		advancedOptionsPanel.add(launcherPathField, gbc_launcherPathField);
		launcherPathField.setColumns(10);

		launcherPathBrowseButton = new JButton("Browse...");
		GridBagConstraints gbc_launcherPathBrowseButton = new GridBagConstraints();
		gbc_launcherPathBrowseButton.insets = new Insets(0, 0, 5, 0);
		gbc_launcherPathBrowseButton.gridx = 2;
		gbc_launcherPathBrowseButton.gridy = 8;
		advancedOptionsPanel.add(launcherPathBrowseButton, gbc_launcherPathBrowseButton);

		JLabel createdVersionNameLabel = new JLabel("Created version name");
		GridBagConstraints gbc_createdVersionNameLabel = new GridBagConstraints();
		gbc_createdVersionNameLabel.anchor = GridBagConstraints.EAST;
		gbc_createdVersionNameLabel.insets = new Insets(0, 0, 5, 5);
		gbc_createdVersionNameLabel.gridx = 0;
		gbc_createdVersionNameLabel.gridy = 9;
		advancedOptionsPanel.add(createdVersionNameLabel, gbc_createdVersionNameLabel);

		createdVersionNameField = new JTextField();
		GridBagConstraints gbc_createdVersionNameField = new GridBagConstraints();
		gbc_createdVersionNameField.insets = new Insets(0, 0, 5, 5);
		gbc_createdVersionNameField.fill = GridBagConstraints.HORIZONTAL;
		gbc_createdVersionNameField.gridx = 1;
		gbc_createdVersionNameField.gridy = 9;
		advancedOptionsPanel.add(createdVersionNameField, gbc_createdVersionNameField);
		createdVersionNameField.setColumns(10);



		mainFrame.pack();
		mainFrame.setLocationRelativeTo(null);



		// TODO: load defaults and retrieved values
		// - set launcher path to .minecraft folder
		// - set created version name to same as installation name? (unless modified by user)
		// - set game directory from install name (unless modified by user)
		// - when pack URL is added, use metadata to populate install name, mod loader, mc version, loader version, etc.
		// - set default pack URL to one appended to .jar?
		//   - then a webpage can be used to append them and give the user a customised jar (or .exe with fabric-installer-native-bootstrap)

		// TODO: implement browse buttons
		// TODO: implement generated path / .minecraft buttons
		// - warn if any path (other than .minecraft) already exists
		// - warn if mods folder or packwiz.json in .minecraft is non-empty?
		// TODO: implement refresh button

		// Dynamically listen for em all i guess
		var loaderGroupElements = loaderGroup.getElements();
		while (loaderGroupElements.hasMoreElements()) {
			var loaderButton = (JRadioButton) loaderGroupElements.nextElement();
			loaderButton.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() != ItemEvent.SELECTED) return;

					var button = (JRadioButton) e.getItem();
					Class<LoaderMetadataGetter> loaderClass = (Class<LoaderMetadataGetter>) button.getClientProperty("MCLoaderClass");

					if (loaderButton == null) return;

					// Clear MC versions and loader versions, disable em until they load again
					minecraftVersionComboBox.removeAllItems();
					loaderVersionComboBox.removeAllItems();
					minecraftVersionComboBox.setEnabled(false);
					loaderVersionComboBox.setEnabled(false);
					try {
						loaderMetaGetter = loaderClass.getDeclaredConstructor().newInstance();
						loadMCVersions();
					} catch (InstantiationException | NoSuchMethodException | InvocationTargetException |
							 IllegalAccessException ex) { // These shouldn't happen but :P
						throw new RuntimeException(ex);
					}
				}
			});
		}

		minecraftVersionComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadLoaderVersions();
			}
		});

		// TODO: when Fabric selected:
		// - when install loader checked; list all minecraft + fabric loader versions (snapshots checkbox?)
		// - when install loader checked, hide the "can't see the version you want" text
		// - when install loader unchecked, display a list of installed versions

		// TODO: when Forge selected:
		// - display a list of installed versions, ideally filtered by Forge and minecraft version?
		// - display a link to download the Forge installer

		// TODO: when None selected:
		// - display a list of installed versions

		// - could display the loader and minecraft versions used by the pack even when not automatically installing

		// TODO: implement next page (with install progress label + spinner)
		// TODO: error display
		// TODO: self version check?

		nextButton.addActionListener(e -> {
			try {
				Main.main(new String[]{});
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		});

		launcherPathField.setText(PathUtils.getMinecraftPath().toString());
		gameDirectoryGeneratedPathButton.addActionListener(e -> {
			// TODO: when empty, make error dialog
			gameDirectoryField.setText(PathUtils.getGeneratedPath(installationNameField.getText()).toString());
		});
		gameDirectoryUseMcFolderButton.addActionListener(e -> {
			gameDirectoryField.setText(launcherPathField.getText());
		});

		// Listen for paste
		var pasteAction = packUrlField.getActionMap().get("paste-from-clipboard");
		packUrlField.getActionMap().put("paste-from-clipboard", new PasteProxy(pasteAction) {
			@Override
			public void textPasted(ActionEvent e) {
				urlDebouncer.finish(); // This is called after the document listener
				// So we can just finish it here
			}
		});
		packUrlField.getDocument().addDocumentListener(new DocumentListener() {
			// TODO: Lil timer to wait for the user to stop typing and DL the file
			// OR try to parse it immediately after paste (which is why I have the textPasted bool)
			@Override
			public void insertUpdate(DocumentEvent e) {
				validate();
				urlDebouncer.call();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				validate();
				urlDebouncer.call();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				validate();
				urlDebouncer.call();
			}
		});
	}

	// lil hack to know when something was just pasted
	private abstract class PasteProxy extends AbstractAction {
		private Action action;

		public PasteProxy(Action action) {
			this.action = action;
		}

		@Override
		public void actionPerformed(ActionEvent e) {

			action.actionPerformed(e);
			textPasted(e);
		}

		public abstract void textPasted(ActionEvent e);
	}

	private void setLoading(boolean loading) {
		loadingBar.setVisible(loading);
		if (loading)
			warningLabel.setVisible(false);
	}

	private void setError(String error) {
		if (error != null && !error.isEmpty()) {
			warningLabel.setText(error);
		}
		warningLabel.setVisible(error != null && !error.isEmpty());
	}

	private void setValid(boolean status) {
		installationNameField.setEnabled(status);
		refreshVersionsButton.setEnabled(status);
		var loaders = loaderGroup.getElements();
		AbstractButton button;
		while (loaders.hasMoreElements()) {
			button = loaders.nextElement();
			button.setEnabled(status);
		}
		if (!status) {
			loaderGroup.clearSelection();
		}
		checkboxInstallLoader.setEnabled(status);
		//minecraftVersionComboBox.setEnabled(status);
		//loaderVersionComboBox.setEnabled(status);
		gameDirectoryField.setEnabled(status);
		gameDirectoryBrowseButton.setEnabled(status);
		gameDirectoryGeneratedPathButton.setEnabled(status);
		launcherPathField.setEnabled(status);
		gameDirectoryUseMcFolderButton.setEnabled(status);

		gameDirectoryUseMcFolderButton.setEnabled(status);
		launcherPathBrowseButton.setEnabled(status);
		createdVersionNameField.setEnabled(status);
		nextButton.setEnabled(status);
	}

	private void validate() {
		setValid(false); // If it's valid it will be set when the pack downloads and works
		// So we just set it as invalid from the start until it works
		if (packUrlField.getText().isEmpty()) {
			setError("Pack URL is required");
			return;
		}

		if (!urlValidator.isValid(packUrlField.getText())) {
			setError("Invalid Pack URL");
			return;
		}

		setError(null);
	}



	private void onPackDataLoaded(PackMetadataRetriever.Data data) {
		setValid(true);
		setLoading(false);

		installationNameField.setText(data.getName());
		var vers = data.getVersions();
		minecraftVersionComboBox.setSelectedItem(vers.get("minecraft"));
		createdVersionNameField.setText(PathUtils.slugify(data.getName()));
		gameDirectoryField.setText(PathUtils.getGeneratedPath(installationNameField.getText()).toString());

		for (var loader : loaderRadioButtons.entrySet()) {
			if (vers.containsKey(loader.getKey())) {
				loader.getValue().setSelected(true); // This will automatically handle version setting, etc
				break;
			}
		}
	}


	private void downloadPackFile(String url) {
		if (!urlValidator.isValid(url)) {
			setError("Invalid pack file URL");
			return;
		}

		try {
			var fileUri = new URI(url);
			var willUpdate = metadataRetriever.update(fileUri);

			if (willUpdate) {
				setLoading(true);
			}
		} catch (URISyntaxException e) {
			// Ignore
		}
	}

	private void loadMCVersions() {
		if (loaderMetaGetter == null) return; // Do nothing if no loader obtained

		setLoading(true);

		var thread = new SwingWorker<List<String>, Void>() {
			@Override
			public List<String> doInBackground() {
				return loaderMetaGetter.getMinecraftVersions(false); // TODO: Checkbox?
			}

			@Override
			protected void done() {
				setLoading(false);

				minecraftVersionComboBox.setEnabled(true); // Gotta enable it to remove everything - sigh
				minecraftVersionComboBox.removeAllItems();

				// Remove listeners first... If not they'll get called as we add everything
				final ActionListener[] actionListeners = minecraftVersionComboBox.getActionListeners();
				for (final ActionListener listener : actionListeners)
					minecraftVersionComboBox.removeActionListener(listener);

				List<String> versions = null;
				try {
					versions = this.get();
					for (int i = 0; i < versions.size(); i++) {
						var version = versions.get(i);
						minecraftVersionComboBox.addItem(version);
					}
				} catch (InterruptedException | ExecutionException e) {
					//throw new RuntimeException(e);
					setError("Error while loading " + loaderMetaGetter.getLoaderName() + " MC versions: " + e.getLocalizedMessage());
				}

				// We re-add the listeners
				for (final ActionListener listener : actionListeners)
					minecraftVersionComboBox.addActionListener(listener);

				// Only set the selected if it's our loader
				var metadata = metadataRetriever.getData();
				if (versions != null && metadata != null && metadata.getVersions().containsKey("minecraft") && metadata.getVersions().containsKey(loaderMetaGetter.getLoaderName()) // Only match if the loader is the same!
					&& versions.contains(metadata.getVersions().get("minecraft"))) {
					minecraftVersionComboBox.setSelectedIndex(versions.indexOf(metadata.getVersions().get("minecraft")));
				} else if (minecraftVersionComboBox.getItemCount() > 0) { // Else we set the first one so the event gets called
					minecraftVersionComboBox.setSelectedIndex(0);
				} else {
					minecraftVersionComboBox.setEnabled(false);
				}
			}
		};

		thread.execute();
	}

	private void loadLoaderVersions() {
		if (loaderMetaGetter == null) return; // Do nothing if no loader obtained
		String mcVersion = (String) minecraftVersionComboBox.getSelectedItem();
		if (mcVersion == null) return; // Do nothing if mc version is not selected

		setLoading(true);

		var thread = new SwingWorker<List<String>, Void>() {
			@Override
			protected List<String> doInBackground() {
				return loaderMetaGetter.getLoaderVersions(mcVersion, false); // TODO: Checkbox?
			}

			@Override
			protected void done() {
				setLoading(false);
				if (this.isCancelled()) return;

				loaderVersionComboBox.setEnabled(true); // Gotta enable it to remove everything - sigh
				loaderVersionComboBox.removeAllItems();

				// Remove listeners first... If not they'll get called as we add everything
				final ActionListener[] actionListeners = loaderVersionComboBox.getActionListeners();
				for (final ActionListener listener : actionListeners)
					loaderVersionComboBox.removeActionListener(listener);

				List<String> versions = null;
				try {
					versions = this.get();

					for (int i = 0; i < versions.size(); i++) {
						var version = versions.get(i);
						loaderVersionComboBox.addItem(version);
						if (metadataRetriever.getData() != null && version.equals(metadataRetriever.getData().getVersions().get(loaderMetaGetter.getLoaderName()))) {
							loaderVersionComboBox.setSelectedIndex(i);
						}
					}
					loaderVersionComboBox.setEnabled(true);

				} catch (InterruptedException | ExecutionException e) {
					//throw new RuntimeException(e);
					setError("Error while loading " + loaderMetaGetter.getLoaderName() + " versions: " + e.getLocalizedMessage());
				}

				// We re-add the listeners
				for (final ActionListener listener : actionListeners)
					loaderVersionComboBox.addActionListener(listener);

				// Only set the selected if it's our loader
				var metadata = metadataRetriever.getData();
				if (versions != null && metadata != null && metadata.getVersions().containsKey(loaderMetaGetter.getLoaderName())
					&& versions.contains(metadata.getVersions().get(loaderMetaGetter.getLoaderName()))) {
					loaderVersionComboBox.setSelectedIndex(versions.indexOf(metadata.getVersions().get(loaderMetaGetter.getLoaderName())));
				} else if (loaderVersionComboBox.getItemCount() > 0) { // Else we set the first one so the event gets called
					loaderVersionComboBox.setSelectedIndex(0);
				} else {
					loaderVersionComboBox.setEnabled(false);
				}
			}
		};

		thread.execute();
	}
}
