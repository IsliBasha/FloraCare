package com.floracare.app.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.floracare.app.domain.model.HumidityNeed;
import com.floracare.app.domain.model.LightNeed;
import com.floracare.app.domain.model.Toxicity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalStateException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SpeciesDao_Impl implements SpeciesDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SpeciesEntity> __insertionAdapterOfSpeciesEntity;

  private final Converters __converters = new Converters();

  public SpeciesDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSpeciesEntity = new EntityInsertionAdapter<SpeciesEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `species` (`id`,`scientificName`,`commonName`,`waterFrequencyDays`,`lightNeed`,`humidityNeed`,`tempMinC`,`tempMaxC`,`toxicity`,`careNotes`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SpeciesEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getScientificName());
        statement.bindString(3, entity.getCommonName());
        statement.bindLong(4, entity.getWaterFrequencyDays());
        final String _tmp = __converters.lightToString(entity.getLightNeed());
        if (_tmp == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, _tmp);
        }
        final String _tmp_1 = __converters.humidityToString(entity.getHumidityNeed());
        if (_tmp_1 == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, _tmp_1);
        }
        statement.bindDouble(7, entity.getTempMinC());
        statement.bindDouble(8, entity.getTempMaxC());
        final String _tmp_2 = __converters.toxicityToString(entity.getToxicity());
        if (_tmp_2 == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, _tmp_2);
        }
        statement.bindString(10, entity.getCareNotes());
      }
    };
  }

  @Override
  public Object upsertAll(final List<SpeciesEntity> species,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSpeciesEntity.insert(species);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object findById(final String id, final Continuation<? super SpeciesEntity> $completion) {
    final String _sql = "SELECT * FROM species WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SpeciesEntity>() {
      @Override
      @Nullable
      public SpeciesEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfScientificName = CursorUtil.getColumnIndexOrThrow(_cursor, "scientificName");
          final int _cursorIndexOfCommonName = CursorUtil.getColumnIndexOrThrow(_cursor, "commonName");
          final int _cursorIndexOfWaterFrequencyDays = CursorUtil.getColumnIndexOrThrow(_cursor, "waterFrequencyDays");
          final int _cursorIndexOfLightNeed = CursorUtil.getColumnIndexOrThrow(_cursor, "lightNeed");
          final int _cursorIndexOfHumidityNeed = CursorUtil.getColumnIndexOrThrow(_cursor, "humidityNeed");
          final int _cursorIndexOfTempMinC = CursorUtil.getColumnIndexOrThrow(_cursor, "tempMinC");
          final int _cursorIndexOfTempMaxC = CursorUtil.getColumnIndexOrThrow(_cursor, "tempMaxC");
          final int _cursorIndexOfToxicity = CursorUtil.getColumnIndexOrThrow(_cursor, "toxicity");
          final int _cursorIndexOfCareNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "careNotes");
          final SpeciesEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpScientificName;
            _tmpScientificName = _cursor.getString(_cursorIndexOfScientificName);
            final String _tmpCommonName;
            _tmpCommonName = _cursor.getString(_cursorIndexOfCommonName);
            final int _tmpWaterFrequencyDays;
            _tmpWaterFrequencyDays = _cursor.getInt(_cursorIndexOfWaterFrequencyDays);
            final LightNeed _tmpLightNeed;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfLightNeed)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfLightNeed);
            }
            final LightNeed _tmp_1 = __converters.stringToLight(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'com.floracare.app.domain.model.LightNeed', but it was NULL.");
            } else {
              _tmpLightNeed = _tmp_1;
            }
            final HumidityNeed _tmpHumidityNeed;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfHumidityNeed)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfHumidityNeed);
            }
            final HumidityNeed _tmp_3 = __converters.stringToHumidity(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'com.floracare.app.domain.model.HumidityNeed', but it was NULL.");
            } else {
              _tmpHumidityNeed = _tmp_3;
            }
            final float _tmpTempMinC;
            _tmpTempMinC = _cursor.getFloat(_cursorIndexOfTempMinC);
            final float _tmpTempMaxC;
            _tmpTempMaxC = _cursor.getFloat(_cursorIndexOfTempMaxC);
            final Toxicity _tmpToxicity;
            final String _tmp_4;
            if (_cursor.isNull(_cursorIndexOfToxicity)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getString(_cursorIndexOfToxicity);
            }
            final Toxicity _tmp_5 = __converters.stringToToxicity(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'com.floracare.app.domain.model.Toxicity', but it was NULL.");
            } else {
              _tmpToxicity = _tmp_5;
            }
            final String _tmpCareNotes;
            _tmpCareNotes = _cursor.getString(_cursorIndexOfCareNotes);
            _result = new SpeciesEntity(_tmpId,_tmpScientificName,_tmpCommonName,_tmpWaterFrequencyDays,_tmpLightNeed,_tmpHumidityNeed,_tmpTempMinC,_tmpTempMaxC,_tmpToxicity,_tmpCareNotes);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SpeciesEntity>> observeAll() {
    final String _sql = "SELECT * FROM species ORDER BY commonName ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"species"}, new Callable<List<SpeciesEntity>>() {
      @Override
      @NonNull
      public List<SpeciesEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfScientificName = CursorUtil.getColumnIndexOrThrow(_cursor, "scientificName");
          final int _cursorIndexOfCommonName = CursorUtil.getColumnIndexOrThrow(_cursor, "commonName");
          final int _cursorIndexOfWaterFrequencyDays = CursorUtil.getColumnIndexOrThrow(_cursor, "waterFrequencyDays");
          final int _cursorIndexOfLightNeed = CursorUtil.getColumnIndexOrThrow(_cursor, "lightNeed");
          final int _cursorIndexOfHumidityNeed = CursorUtil.getColumnIndexOrThrow(_cursor, "humidityNeed");
          final int _cursorIndexOfTempMinC = CursorUtil.getColumnIndexOrThrow(_cursor, "tempMinC");
          final int _cursorIndexOfTempMaxC = CursorUtil.getColumnIndexOrThrow(_cursor, "tempMaxC");
          final int _cursorIndexOfToxicity = CursorUtil.getColumnIndexOrThrow(_cursor, "toxicity");
          final int _cursorIndexOfCareNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "careNotes");
          final List<SpeciesEntity> _result = new ArrayList<SpeciesEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SpeciesEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpScientificName;
            _tmpScientificName = _cursor.getString(_cursorIndexOfScientificName);
            final String _tmpCommonName;
            _tmpCommonName = _cursor.getString(_cursorIndexOfCommonName);
            final int _tmpWaterFrequencyDays;
            _tmpWaterFrequencyDays = _cursor.getInt(_cursorIndexOfWaterFrequencyDays);
            final LightNeed _tmpLightNeed;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfLightNeed)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfLightNeed);
            }
            final LightNeed _tmp_1 = __converters.stringToLight(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'com.floracare.app.domain.model.LightNeed', but it was NULL.");
            } else {
              _tmpLightNeed = _tmp_1;
            }
            final HumidityNeed _tmpHumidityNeed;
            final String _tmp_2;
            if (_cursor.isNull(_cursorIndexOfHumidityNeed)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getString(_cursorIndexOfHumidityNeed);
            }
            final HumidityNeed _tmp_3 = __converters.stringToHumidity(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'com.floracare.app.domain.model.HumidityNeed', but it was NULL.");
            } else {
              _tmpHumidityNeed = _tmp_3;
            }
            final float _tmpTempMinC;
            _tmpTempMinC = _cursor.getFloat(_cursorIndexOfTempMinC);
            final float _tmpTempMaxC;
            _tmpTempMaxC = _cursor.getFloat(_cursorIndexOfTempMaxC);
            final Toxicity _tmpToxicity;
            final String _tmp_4;
            if (_cursor.isNull(_cursorIndexOfToxicity)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getString(_cursorIndexOfToxicity);
            }
            final Toxicity _tmp_5 = __converters.stringToToxicity(_tmp_4);
            if (_tmp_5 == null) {
              throw new IllegalStateException("Expected NON-NULL 'com.floracare.app.domain.model.Toxicity', but it was NULL.");
            } else {
              _tmpToxicity = _tmp_5;
            }
            final String _tmpCareNotes;
            _tmpCareNotes = _cursor.getString(_cursorIndexOfCareNotes);
            _item = new SpeciesEntity(_tmpId,_tmpScientificName,_tmpCommonName,_tmpWaterFrequencyDays,_tmpLightNeed,_tmpHumidityNeed,_tmpTempMinC,_tmpTempMaxC,_tmpToxicity,_tmpCareNotes);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object count(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM species";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
