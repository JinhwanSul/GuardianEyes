package com.google.ar.core.examples.java.helloar.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataSaver {

  private File path;

  public DataSaver(File path) {
    this.path = path;
  }

  public void saveData(String[] dataString) {
    String outputPath = path.getAbsolutePath();
    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    String filePath = outputPath + "/data_" + timeStamp + ".csv";
    File firstYFile = new File(filePath);

    String[][] split = new String[dataString.length][];
    for(int i = 0; i < dataString.length; ++i)
      split[i] = dataString[i].split("\n");
    String output = "";
    for(int i = 0; i < split[0].length; ++i) {
      String tmp = "";
      for(int j = 0; j < split.length; ++j) {
        if(j == 0) tmp += split[j][i];
        else tmp += "," + split[j][i];
      }
      if(i == 0) output += tmp;
      else output += "\n" + tmp;
    }

    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(firstYFile));
      writer.write(output);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
