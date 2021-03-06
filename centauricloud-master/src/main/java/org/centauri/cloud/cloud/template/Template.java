package org.centauri.cloud.cloud.template;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.centauri.cloud.cloud.Cloud;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.centauri.cloud.common.network.config.TemplateConfig;

@RequiredArgsConstructor
public class Template {
	
	@Getter private final String name;
	@Getter private final File dir;
	@Getter private final File config;
	@Getter private int minServersFree;
	@Getter private int maxPlayers;
	@Getter private TemplateConfig templateConfig;
	@Getter private Map<File, File> dependencies = new HashMap<>();
	
	public void loadConfig() throws Exception {
		this.templateConfig = new TemplateConfig(this.dir);
		this.minServersFree = (int) this.templateConfig.getOrElse("template.minServersFree", 1);
		this.maxPlayers = (int) this.templateConfig.getOrElse("template.maxPlayers", 16);
	}
	
	public void loadSharedFiles() throws Exception {
		DependencieResolver.resolveDependencies(this);
	}
	
	public void build() throws Exception {
		this.getDependencies().forEach((src, dest) -> {
			try {
				if (!src.exists()) {
					Cloud.getLogger().warn("Cannot find shared file {}({}) for template {}", src.getName(), src.getAbsolutePath(), this.name);
					return;
				}

				if(dest.exists())
					FileUtils.deleteQuietly(dest);

				if (src.isDirectory()) {
					this.copyFolder(src, dest);
				} else {
					Files.copy(src.toPath(), dest.toPath());
				}
				
			} catch (Exception ex) {
				Cloud.getLogger().catching(ex);
			}
		});
	}
	
	@SneakyThrows
	public void compress() {
		compressZipfile(this.getDir().getPath() + "/", Cloud.getInstance().getTmpDir().getPath() + "/" + this.name + ".zip");
		Cloud.getLogger().info("Compressed template {} into a zip file!", this.name);
	}
	
	private void compressZipfile(String sourceDir, String outputFile) throws Exception {
		try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputFile))) {
			compressDirectoryToZipfile(sourceDir, sourceDir, zos);
		}
	}

	private void compressDirectoryToZipfile(String rootDir, String sourceDir, ZipOutputStream out) throws Exception {
		for (File file : new File(sourceDir).listFiles()) {
			if (file.isDirectory()) {
				compressDirectoryToZipfile(rootDir, sourceDir + file.getName() + "/", out);
			} else {
				ZipEntry entry = new ZipEntry(sourceDir.replace(rootDir, "") + file.getName());
				out.putNextEntry(entry);

				try (FileInputStream in = new FileInputStream(sourceDir + file.getName())) {
					IOUtils.copy(in, out);
				}
			}
		}
	}
	
	public void deleteRecursive(File path) {
		File[] c = path.listFiles();
		for (File file : c) {
			if (file.isDirectory()) {
				deleteRecursive(file);
				file.delete();
			} else {
				file.delete();
			}
		}
		path.delete();
	}

	@SneakyThrows
	static private void copyFolder(File src, File dest) {
		if (src == null || dest == null)
			return;

		if (!src.isDirectory()) {
			FileUtils.copyFile(src, dest);
			return;
		}

		if (dest.exists()) {
			if (!dest.isDirectory()) {
				return;
			}
		} else {
			dest.mkdir();
		}

		if (src.listFiles() == null || src.listFiles().length == 0)
			return;

		for (File file : src.listFiles()) {
			File fileDest = new File(dest, file.getName());
			if (file.isDirectory()) {
				copyFolder(file, fileDest);
			} else {
				if (fileDest.exists())
					continue;
				Files.copy(file.toPath(), fileDest.toPath());
			}
		}
	}

}