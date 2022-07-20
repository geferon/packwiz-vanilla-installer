package link.infra.packwiz.vanillainstaller.meta;

import org.apache.commons.io.input.BOMInputStream;
import org.intellij.lang.annotations.JdkConstants;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForgeMetadata implements LoaderMetadataGetter {
    private static final URL MAVEN_METADATA;

    static {
        try {
            MAVEN_METADATA = new URL("https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    private static final Pattern VERSION_REG = Pattern.compile("(.*?)-(.*)", Pattern.CASE_INSENSITIVE);

    private final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    public ForgeMetadata() {
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public String getLoaderName() {
        return "forge";
    }

    private List<String> versionsCache = null;
    private List<String> getForgeVersions() throws IOException, ParserConfigurationException, SAXException {
        if (versionsCache != null) return versionsCache;
        var connection = (HttpURLConnection) MAVEN_METADATA.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.77 Safari/537.36");
        connection.connect();

        var db = dbf.newDocumentBuilder();
        var doc = db.parse(new BOMInputStream(connection.getInputStream()));
        doc.getDocumentElement().normalize();

        var elements = doc.getElementsByTagName("version");
        var versions = new ArrayList<String>();
        for (int i = 0; i < elements.getLength(); i++) {
            var node = elements.item(i);
            versions.add(node.getTextContent());
        }
        versionsCache = versions;
        return versions;
    }

    @Override
    public List<String> getMinecraftVersions(boolean stableOnly) {
        try {
            return getForgeVersions().stream()
                    .map(VERSION_REG::matcher)
                    .filter(Matcher::find)
                    .map(e -> e.group(1))
                    .distinct()
                    .toList();
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> getLoaderVersions(String mcVersion, boolean stableOnly) {
        try {
            return getForgeVersions().stream()
                    .map(VERSION_REG::matcher)
                    .filter(e -> e.find() && e.group(1).equals(mcVersion))
                    .map(e -> e.group(2))
                    .toList();
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }
}
