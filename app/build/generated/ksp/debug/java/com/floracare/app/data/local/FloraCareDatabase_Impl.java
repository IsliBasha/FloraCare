package com.floracare.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class FloraCareDatabase_Impl extends FloraCareDatabase {
  private volatile PlantDao _plantDao;

  private volatile SpeciesDao _speciesDao;

  private volatile CareTaskDao _careTaskDao;

  private volatile CareLogDao _careLogDao;

  private volatile JournalDao _journalDao;

  private volatile DiagnosisDao _diagnosisDao;

  private volatile WeatherDao _weatherDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `plant` (`id` TEXT NOT NULL, `nickname` TEXT NOT NULL, `speciesId` TEXT, `locationTag` TEXT NOT NULL, `acquiredAt` INTEGER NOT NULL, `coverPhotoUri` TEXT, `notes` TEXT NOT NULL, `archived` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`speciesId`) REFERENCES `species`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plant_speciesId` ON `plant` (`speciesId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `species` (`id` TEXT NOT NULL, `scientificName` TEXT NOT NULL, `commonName` TEXT NOT NULL, `waterFrequencyDays` INTEGER NOT NULL, `lightNeed` TEXT NOT NULL, `humidityNeed` TEXT NOT NULL, `tempMinC` REAL NOT NULL, `tempMaxC` REAL NOT NULL, `toxicity` TEXT NOT NULL, `careNotes` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `care_task` (`id` TEXT NOT NULL, `plantId` TEXT NOT NULL, `type` TEXT NOT NULL, `scheduledAt` INTEGER NOT NULL, `completedAt` INTEGER, `snoozedUntil` INTEGER, `source` TEXT NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`plantId`) REFERENCES `plant`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_care_task_plantId` ON `care_task` (`plantId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_care_task_scheduledAt` ON `care_task` (`scheduledAt`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `care_log` (`id` TEXT NOT NULL, `plantId` TEXT NOT NULL, `taskType` TEXT NOT NULL, `performedAt` INTEGER NOT NULL, `soilMoistureNote` TEXT, `userNote` TEXT, PRIMARY KEY(`id`), FOREIGN KEY(`plantId`) REFERENCES `plant`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_care_log_plantId` ON `care_log` (`plantId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_care_log_performedAt` ON `care_log` (`performedAt`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `journal_entry` (`id` TEXT NOT NULL, `plantId` TEXT NOT NULL, `photoUri` TEXT NOT NULL, `capturedAt` INTEGER NOT NULL, `note` TEXT NOT NULL, `heightCm` REAL, PRIMARY KEY(`id`), FOREIGN KEY(`plantId`) REFERENCES `plant`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entry_plantId` ON `journal_entry` (`plantId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_entry_capturedAt` ON `journal_entry` (`capturedAt`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `diagnosis_result` (`id` TEXT NOT NULL, `plantId` TEXT, `photoUri` TEXT NOT NULL, `diagnosisLabel` TEXT NOT NULL, `confidence` REAL NOT NULL, `treatmentSuggestion` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `weather_snapshot` (`id` TEXT NOT NULL, `lat` REAL NOT NULL, `lon` REAL NOT NULL, `recordedAt` INTEGER NOT NULL, `tempC` REAL NOT NULL, `humidityPct` REAL NOT NULL, `rainMm` REAL NOT NULL, `uvIndex` REAL NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_weather_snapshot_recordedAt` ON `weather_snapshot` (`recordedAt`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e6068da087e2ce6d5954ee730698ce3c')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `plant`");
        db.execSQL("DROP TABLE IF EXISTS `species`");
        db.execSQL("DROP TABLE IF EXISTS `care_task`");
        db.execSQL("DROP TABLE IF EXISTS `care_log`");
        db.execSQL("DROP TABLE IF EXISTS `journal_entry`");
        db.execSQL("DROP TABLE IF EXISTS `diagnosis_result`");
        db.execSQL("DROP TABLE IF EXISTS `weather_snapshot`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsPlant = new HashMap<String, TableInfo.Column>(8);
        _columnsPlant.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlant.put("nickname", new TableInfo.Column("nickname", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlant.put("speciesId", new TableInfo.Column("speciesId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlant.put("locationTag", new TableInfo.Column("locationTag", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlant.put("acquiredAt", new TableInfo.Column("acquiredAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlant.put("coverPhotoUri", new TableInfo.Column("coverPhotoUri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlant.put("notes", new TableInfo.Column("notes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlant.put("archived", new TableInfo.Column("archived", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlant = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysPlant.add(new TableInfo.ForeignKey("species", "SET NULL", "NO ACTION", Arrays.asList("speciesId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesPlant = new HashSet<TableInfo.Index>(1);
        _indicesPlant.add(new TableInfo.Index("index_plant_speciesId", false, Arrays.asList("speciesId"), Arrays.asList("ASC")));
        final TableInfo _infoPlant = new TableInfo("plant", _columnsPlant, _foreignKeysPlant, _indicesPlant);
        final TableInfo _existingPlant = TableInfo.read(db, "plant");
        if (!_infoPlant.equals(_existingPlant)) {
          return new RoomOpenHelper.ValidationResult(false, "plant(com.floracare.app.data.local.PlantEntity).\n"
                  + " Expected:\n" + _infoPlant + "\n"
                  + " Found:\n" + _existingPlant);
        }
        final HashMap<String, TableInfo.Column> _columnsSpecies = new HashMap<String, TableInfo.Column>(10);
        _columnsSpecies.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpecies.put("scientificName", new TableInfo.Column("scientificName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpecies.put("commonName", new TableInfo.Column("commonName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpecies.put("waterFrequencyDays", new TableInfo.Column("waterFrequencyDays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpecies.put("lightNeed", new TableInfo.Column("lightNeed", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpecies.put("humidityNeed", new TableInfo.Column("humidityNeed", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpecies.put("tempMinC", new TableInfo.Column("tempMinC", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpecies.put("tempMaxC", new TableInfo.Column("tempMaxC", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpecies.put("toxicity", new TableInfo.Column("toxicity", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSpecies.put("careNotes", new TableInfo.Column("careNotes", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSpecies = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSpecies = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSpecies = new TableInfo("species", _columnsSpecies, _foreignKeysSpecies, _indicesSpecies);
        final TableInfo _existingSpecies = TableInfo.read(db, "species");
        if (!_infoSpecies.equals(_existingSpecies)) {
          return new RoomOpenHelper.ValidationResult(false, "species(com.floracare.app.data.local.SpeciesEntity).\n"
                  + " Expected:\n" + _infoSpecies + "\n"
                  + " Found:\n" + _existingSpecies);
        }
        final HashMap<String, TableInfo.Column> _columnsCareTask = new HashMap<String, TableInfo.Column>(7);
        _columnsCareTask.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCareTask.put("plantId", new TableInfo.Column("plantId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCareTask.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCareTask.put("scheduledAt", new TableInfo.Column("scheduledAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCareTask.put("completedAt", new TableInfo.Column("completedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCareTask.put("snoozedUntil", new TableInfo.Column("snoozedUntil", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCareTask.put("source", new TableInfo.Column("source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCareTask = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysCareTask.add(new TableInfo.ForeignKey("plant", "CASCADE", "NO ACTION", Arrays.asList("plantId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesCareTask = new HashSet<TableInfo.Index>(2);
        _indicesCareTask.add(new TableInfo.Index("index_care_task_plantId", false, Arrays.asList("plantId"), Arrays.asList("ASC")));
        _indicesCareTask.add(new TableInfo.Index("index_care_task_scheduledAt", false, Arrays.asList("scheduledAt"), Arrays.asList("ASC")));
        final TableInfo _infoCareTask = new TableInfo("care_task", _columnsCareTask, _foreignKeysCareTask, _indicesCareTask);
        final TableInfo _existingCareTask = TableInfo.read(db, "care_task");
        if (!_infoCareTask.equals(_existingCareTask)) {
          return new RoomOpenHelper.ValidationResult(false, "care_task(com.floracare.app.data.local.CareTaskEntity).\n"
                  + " Expected:\n" + _infoCareTask + "\n"
                  + " Found:\n" + _existingCareTask);
        }
        final HashMap<String, TableInfo.Column> _columnsCareLog = new HashMap<String, TableInfo.Column>(6);
        _columnsCareLog.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCareLog.put("plantId", new TableInfo.Column("plantId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCareLog.put("taskType", new TableInfo.Column("taskType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCareLog.put("performedAt", new TableInfo.Column("performedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCareLog.put("soilMoistureNote", new TableInfo.Column("soilMoistureNote", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCareLog.put("userNote", new TableInfo.Column("userNote", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCareLog = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysCareLog.add(new TableInfo.ForeignKey("plant", "CASCADE", "NO ACTION", Arrays.asList("plantId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesCareLog = new HashSet<TableInfo.Index>(2);
        _indicesCareLog.add(new TableInfo.Index("index_care_log_plantId", false, Arrays.asList("plantId"), Arrays.asList("ASC")));
        _indicesCareLog.add(new TableInfo.Index("index_care_log_performedAt", false, Arrays.asList("performedAt"), Arrays.asList("ASC")));
        final TableInfo _infoCareLog = new TableInfo("care_log", _columnsCareLog, _foreignKeysCareLog, _indicesCareLog);
        final TableInfo _existingCareLog = TableInfo.read(db, "care_log");
        if (!_infoCareLog.equals(_existingCareLog)) {
          return new RoomOpenHelper.ValidationResult(false, "care_log(com.floracare.app.data.local.CareLogEntity).\n"
                  + " Expected:\n" + _infoCareLog + "\n"
                  + " Found:\n" + _existingCareLog);
        }
        final HashMap<String, TableInfo.Column> _columnsJournalEntry = new HashMap<String, TableInfo.Column>(6);
        _columnsJournalEntry.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJournalEntry.put("plantId", new TableInfo.Column("plantId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJournalEntry.put("photoUri", new TableInfo.Column("photoUri", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJournalEntry.put("capturedAt", new TableInfo.Column("capturedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJournalEntry.put("note", new TableInfo.Column("note", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsJournalEntry.put("heightCm", new TableInfo.Column("heightCm", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysJournalEntry = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysJournalEntry.add(new TableInfo.ForeignKey("plant", "CASCADE", "NO ACTION", Arrays.asList("plantId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesJournalEntry = new HashSet<TableInfo.Index>(2);
        _indicesJournalEntry.add(new TableInfo.Index("index_journal_entry_plantId", false, Arrays.asList("plantId"), Arrays.asList("ASC")));
        _indicesJournalEntry.add(new TableInfo.Index("index_journal_entry_capturedAt", false, Arrays.asList("capturedAt"), Arrays.asList("ASC")));
        final TableInfo _infoJournalEntry = new TableInfo("journal_entry", _columnsJournalEntry, _foreignKeysJournalEntry, _indicesJournalEntry);
        final TableInfo _existingJournalEntry = TableInfo.read(db, "journal_entry");
        if (!_infoJournalEntry.equals(_existingJournalEntry)) {
          return new RoomOpenHelper.ValidationResult(false, "journal_entry(com.floracare.app.data.local.JournalEntryEntity).\n"
                  + " Expected:\n" + _infoJournalEntry + "\n"
                  + " Found:\n" + _existingJournalEntry);
        }
        final HashMap<String, TableInfo.Column> _columnsDiagnosisResult = new HashMap<String, TableInfo.Column>(7);
        _columnsDiagnosisResult.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiagnosisResult.put("plantId", new TableInfo.Column("plantId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiagnosisResult.put("photoUri", new TableInfo.Column("photoUri", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiagnosisResult.put("diagnosisLabel", new TableInfo.Column("diagnosisLabel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiagnosisResult.put("confidence", new TableInfo.Column("confidence", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiagnosisResult.put("treatmentSuggestion", new TableInfo.Column("treatmentSuggestion", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDiagnosisResult.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDiagnosisResult = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDiagnosisResult = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoDiagnosisResult = new TableInfo("diagnosis_result", _columnsDiagnosisResult, _foreignKeysDiagnosisResult, _indicesDiagnosisResult);
        final TableInfo _existingDiagnosisResult = TableInfo.read(db, "diagnosis_result");
        if (!_infoDiagnosisResult.equals(_existingDiagnosisResult)) {
          return new RoomOpenHelper.ValidationResult(false, "diagnosis_result(com.floracare.app.data.local.DiagnosisResultEntity).\n"
                  + " Expected:\n" + _infoDiagnosisResult + "\n"
                  + " Found:\n" + _existingDiagnosisResult);
        }
        final HashMap<String, TableInfo.Column> _columnsWeatherSnapshot = new HashMap<String, TableInfo.Column>(8);
        _columnsWeatherSnapshot.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherSnapshot.put("lat", new TableInfo.Column("lat", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherSnapshot.put("lon", new TableInfo.Column("lon", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherSnapshot.put("recordedAt", new TableInfo.Column("recordedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherSnapshot.put("tempC", new TableInfo.Column("tempC", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherSnapshot.put("humidityPct", new TableInfo.Column("humidityPct", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherSnapshot.put("rainMm", new TableInfo.Column("rainMm", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWeatherSnapshot.put("uvIndex", new TableInfo.Column("uvIndex", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWeatherSnapshot = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWeatherSnapshot = new HashSet<TableInfo.Index>(1);
        _indicesWeatherSnapshot.add(new TableInfo.Index("index_weather_snapshot_recordedAt", false, Arrays.asList("recordedAt"), Arrays.asList("ASC")));
        final TableInfo _infoWeatherSnapshot = new TableInfo("weather_snapshot", _columnsWeatherSnapshot, _foreignKeysWeatherSnapshot, _indicesWeatherSnapshot);
        final TableInfo _existingWeatherSnapshot = TableInfo.read(db, "weather_snapshot");
        if (!_infoWeatherSnapshot.equals(_existingWeatherSnapshot)) {
          return new RoomOpenHelper.ValidationResult(false, "weather_snapshot(com.floracare.app.data.local.WeatherSnapshotEntity).\n"
                  + " Expected:\n" + _infoWeatherSnapshot + "\n"
                  + " Found:\n" + _existingWeatherSnapshot);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "e6068da087e2ce6d5954ee730698ce3c", "12cfe22494df93d68d5b84975f46ce8f");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "plant","species","care_task","care_log","journal_entry","diagnosis_result","weather_snapshot");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `plant`");
      _db.execSQL("DELETE FROM `species`");
      _db.execSQL("DELETE FROM `care_task`");
      _db.execSQL("DELETE FROM `care_log`");
      _db.execSQL("DELETE FROM `journal_entry`");
      _db.execSQL("DELETE FROM `diagnosis_result`");
      _db.execSQL("DELETE FROM `weather_snapshot`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(PlantDao.class, PlantDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SpeciesDao.class, SpeciesDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CareTaskDao.class, CareTaskDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CareLogDao.class, CareLogDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(JournalDao.class, JournalDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DiagnosisDao.class, DiagnosisDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(WeatherDao.class, WeatherDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public PlantDao plantDao() {
    if (_plantDao != null) {
      return _plantDao;
    } else {
      synchronized(this) {
        if(_plantDao == null) {
          _plantDao = new PlantDao_Impl(this);
        }
        return _plantDao;
      }
    }
  }

  @Override
  public SpeciesDao speciesDao() {
    if (_speciesDao != null) {
      return _speciesDao;
    } else {
      synchronized(this) {
        if(_speciesDao == null) {
          _speciesDao = new SpeciesDao_Impl(this);
        }
        return _speciesDao;
      }
    }
  }

  @Override
  public CareTaskDao careTaskDao() {
    if (_careTaskDao != null) {
      return _careTaskDao;
    } else {
      synchronized(this) {
        if(_careTaskDao == null) {
          _careTaskDao = new CareTaskDao_Impl(this);
        }
        return _careTaskDao;
      }
    }
  }

  @Override
  public CareLogDao careLogDao() {
    if (_careLogDao != null) {
      return _careLogDao;
    } else {
      synchronized(this) {
        if(_careLogDao == null) {
          _careLogDao = new CareLogDao_Impl(this);
        }
        return _careLogDao;
      }
    }
  }

  @Override
  public JournalDao journalDao() {
    if (_journalDao != null) {
      return _journalDao;
    } else {
      synchronized(this) {
        if(_journalDao == null) {
          _journalDao = new JournalDao_Impl(this);
        }
        return _journalDao;
      }
    }
  }

  @Override
  public DiagnosisDao diagnosisDao() {
    if (_diagnosisDao != null) {
      return _diagnosisDao;
    } else {
      synchronized(this) {
        if(_diagnosisDao == null) {
          _diagnosisDao = new DiagnosisDao_Impl(this);
        }
        return _diagnosisDao;
      }
    }
  }

  @Override
  public WeatherDao weatherDao() {
    if (_weatherDao != null) {
      return _weatherDao;
    } else {
      synchronized(this) {
        if(_weatherDao == null) {
          _weatherDao = new WeatherDao_Impl(this);
        }
        return _weatherDao;
      }
    }
  }
}
