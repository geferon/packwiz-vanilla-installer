package link.infra.packwiz.vanillainstaller.meta;

import java.util.List;

public interface LoaderMetadataGetter {
    String getLoaderName();
    List<String> getMinecraftVersions(boolean stableOnly);
    List<String> getLoaderVersions(String mcVersion, boolean stableOnly);
}
