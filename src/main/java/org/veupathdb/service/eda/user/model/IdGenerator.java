package org.veupathdb.service.eda.user.model;

import jakarta.ws.rs.NotFoundException;

import java.util.Random;

public class IdGenerator {

  private static final char[] ID_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
  private static final Random RANDOM = new Random();
  private static final int NUM_ID_CHARS = 7;

  private static String generateRandomId() {
    char[] chars = new char[NUM_ID_CHARS];
    for (int i = 0; i < NUM_ID_CHARS; i++) {
      chars[i] = ID_CHARS[RANDOM.nextInt(ID_CHARS.length)];
    }
    return new String(chars);
  }

  public static String getNextAnalysisId(UserDataFactory dataFactory) {
    while (true) {
      String newId = generateRandomId();
      try {
        // see if an existing analysis has this ID
        dataFactory.getAnalysisById(newId);
      }
      catch (NotFoundException e) {
        // couldn't find one; return this ID
        return newId;
      }
    }
  }
}
