package com.wcucampus.campuscompass;

import android.app.Application;
import com.wcucampus.campuscompass.model.dao.TokenDao;
import com.wcucampus.campuscompass.model.db.CampusInfoDB;
import com.wcucampus.campuscompass.model.entity.Token;
import com.wcucampus.campuscompass.model.utility.TokenPrepper;
import com.wcucampus.campuscompass.model.utility.TokenType;
import com.facebook.stetho.Stetho;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * {@link Application} class.
 */
public class CampusCompassApp extends Application {

  /**
   * The {@link android.arch.persistence.room.Database}.
   */
  CampusInfoDB database;

  @Override
  public void onCreate() {
    super.onCreate();
    initSetupTasks();
  }

  /**
   * Initializes stetho and database.
   */
  private void initSetupTasks() {
    Stetho.initializeWithDefaults(this);
    database = CampusInfoDB.getInstance(this);
    database.getTokenDao();
  }


}
