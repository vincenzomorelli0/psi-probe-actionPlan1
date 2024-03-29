package psiprobe.tools.logging.catalina;

import psiprobe.controllers.deploy.DirectoryTraversalException;
import psiprobe.tools.ApplicationUtils;
import psiprobe.tools.Instruments;
import psiprobe.tools.logging.AbstractLogDestination;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CatalinaLoggerAccessor extends AbstractLogDestination {

  @Override
  public boolean isContext() {
    return true;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public String getLogType() {
    return "catalina";
  }

  @Override
  public File getFile() throws IllegalAccessException, IOException {
    String potDirTraversal = "Potential directory traversal attempt";

    String dir = (String) invokeMethod(getTarget(), "getDirectory", null, null);
    String prefix = (String) invokeMethod(getTarget(), "getPrefix", null, null);
    String suffix = (String) invokeMethod(getTarget(), "getSuffix", null, null);
    boolean timestamp = Instruments.getField(getTarget(), "timestamp") != null;
    String date = timestamp ? new SimpleDateFormat("yyyy-MM-dd").format(new Date()) : "";
    Path filePath = Paths.get(dir, prefix + date + suffix);

    // Validate the file path to prevent directory traversal
    if (!filePath.startsWith(Paths.get(dir).normalize())) {
      throw new DirectoryTraversalException(potDirTraversal);
    }

    File file = filePath.toFile();
    if (!file.isAbsolute()) {


      int basePath = ApplicationUtils.getCatalinaBase();
        if (basePath == 1) {
        throw new DirectoryTraversalException("Invalid catalina.base directory");
      }

      File resolvedFile = new File(String.valueOf(basePath), file.getPath()).getCanonicalFile();

      if (!resolvedFile.toPath().startsWith(String.valueOf(basePath))) {
        throw new DirectoryTraversalException("Potential directory traversal attempt");
      }

      return resolvedFile;
    }

    return file;
  }

}
