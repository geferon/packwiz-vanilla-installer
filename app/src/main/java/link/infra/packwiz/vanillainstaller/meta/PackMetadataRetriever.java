package link.infra.packwiz.vanillainstaller.meta;

import com.moandjiezana.toml.Toml;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public abstract class PackMetadataRetriever {
	private URI packUri = null;
	private Data data = null;
	private MetadataRetrieverTask task = null;

	/**
	 * @return True when new data was fetched
	 */
	public boolean update(URI packUri) {
		assert EventQueue.isDispatchThread();

		if (!packUri.equals(this.packUri)) {
			data = null;
			this.packUri = packUri;

			// TODO: mechanisms for progress/result
			if (task != null) {
				if (!task.isDone()) task.cancel(true);
			}
			task = new MetadataRetrieverTask(packUri);
			task.execute();
			task.addPropertyChangeListener(evt -> {
				if (evt.getPropertyName().equals("state") && evt.getNewValue() == SwingWorker.StateValue.DONE) {
					// Finished!
					try {
						data = task.get();
						onDataUpdated(data);
					} catch (InterruptedException | ExecutionException e) {
						throw new RuntimeException(e); // Shouldn't happen but I'll leave it just in case
					}
				}
			});

			return true;
		}

		return false;
	}

	public abstract void onDataUpdated(Data data);

	public Data getData() {
		return data;
	}

	private static class MetadataRetrieverTask extends SwingWorker<Data, Float> {
		private URI uri;
		public MetadataRetrieverTask(URI uri) {
			this.uri = uri;
		}

		@Override
		protected Data doInBackground() throws Exception {
			InputStream fileStream = null;
			switch (uri.getScheme()) {
				case "http":
				case "https":
					fileStream = uri.toURL().openStream();
					break;
				case "file":
					fileStream = new FileInputStream(Paths.get(uri).toFile());
					break;
				default:
					throw new Exception("Invalid URL scheme supplied: " + uri.getScheme()); // Perhaps a custom exception class?
			}

			var toml = new Toml().read(fileStream);
			return toml.to(Data.class);
		}
	}

	public static class Data {
		// TODO: name, mod loader, mc version, loader version, parsed toml file, validity (throw error if not valid or too new/old)
		String name;
		String author;
		String version;
		Format packFormat;
		IndexFileLoc index;
		Map<String, String> versions;

		public String getName() {
			return name;
		}

		public String getAuthor() {
			return author;
		}

		public String getVersion() {
			return version;
		}

		public Format getPackFormat() {
			return packFormat;
		}

		public IndexFileLoc getIndex() {
			return index;
		}

		public Map<String, String> getVersions() {
			return versions;
		}

		private Data() {}
	}

	public static class IndexFileLoc {
		String file;
		String hashFormat;
		String hash;
	}

	public enum Format {
		DEFAULT("packwiz:1.0.0");

		private String format;

		Format(String format) {
			this.format = format;
		}

		public String getFormat() {
			return this.format;
		}
	}

}
