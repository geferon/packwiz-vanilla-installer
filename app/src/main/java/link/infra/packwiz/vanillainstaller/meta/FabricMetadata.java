package link.infra.packwiz.vanillainstaller.meta;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class FabricMetadata implements LoaderMetadataGetter {
	public static String API_URL = "https://meta.fabricmc.net";
	public static URL GAME_API;
	public static URL YARN_API;

	static {
		try {
			GAME_API = new URL(API_URL + "/v2/versions/game");
			YARN_API = new URL(API_URL + "/v2/versions/yarn");
		} catch (MalformedURLException e) { // Shouldn't happen BUT I NEED TO WRAP THIS SHIT IN IT
			throw new RuntimeException(e);
		}
	}

	private Gson gson = new Gson();

	public FabricMetadata() {}

	public String getLoaderName() {
		return "fabric";
	}

	// TODO: call fabric installer code
	// TODO: could run off-thread with a swingworker?

	// Using Fabric's API for this ensures we don't get versions unsupported by Fabric
	private class GameVersion {
		private String version;
		private boolean stable;
		public String getVersion() {
			return version;
		}
		public void setVersion(String version) {
			this.version = version;
		}
		public boolean isStable() {
			return stable;
		}
		public void setStable(boolean stable) {
			this.stable = stable;
		}
	}

	private List<GameVersion> gameVersions = null;

	@Override
	public List<String> getMinecraftVersions(boolean stableOnly) {
		if (gameVersions == null)
			try {
				Type listType = new TypeToken<ArrayList<GameVersion>>(){}.getType();
				gameVersions = gson.fromJson(new InputStreamReader(GAME_API.openStream()), listType);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		return gameVersions.stream()
				.filter(v -> v.isStable() || !stableOnly)
				.map(GameVersion::getVersion)
				.toList();
	}

	private class YarnVersion {
		private String version;
		private boolean stable;
		private String separator;
		private int build;
		private String maven;
		private String gameVersion;
		public String getVersion() {
			return version;
		}
		public void setVersion(String version) {
			this.version = version;
		}
		public boolean isStable() {
			return stable;
		}
		public void setStable(boolean stable) {
			this.stable = stable;
		}

		public String getSeparator() {
			return separator;
		}

		public void setSeparator(String separator) {
			this.separator = separator;
		}

		public int getBuild() {
			return build;
		}

		public void setBuild(int build) {
			this.build = build;
		}

		public String getMaven() {
			return maven;
		}

		public void setMaven(String maven) {
			this.maven = maven;
		}

		public String getGameVersion() {
			return gameVersion;
		}

		public void setGameVersion(String gameVersion) {
			this.gameVersion = gameVersion;
		}
	}

	private List<YarnVersion> yarnVersions = null;
	@Override
	public List<String> getLoaderVersions(String mcVersion, boolean stableOnly) {
		if (yarnVersions == null)
			try {
				Type listType = new TypeToken<ArrayList<YarnVersion>>(){}.getType();
				yarnVersions = gson.fromJson(new InputStreamReader(YARN_API.openStream()), listType);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		return yarnVersions.stream()
			.filter(v -> (v.isStable() || !stableOnly) && v.getGameVersion().equals(mcVersion))
			.map(YarnVersion::getVersion)
			.toList();
	}
}
