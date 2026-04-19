package com.floracare.app.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.floracare.app.domain.model.CareTaskType;
import com.floracare.app.domain.model.SoilMoistureNote;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalStateException;
import java.lang.Long;
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
import kotlinx.datetime.Instant;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CareLogDao_Impl implements CareLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CareLogEntity> __insertionAdapterOfCareLogEntity;

  private final Converters __converters = new Converters();

  public CareLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCareLogEntity = new EntityInsertionAdapter<CareLogEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `care_log` (`id`,`plantId`,`taskType`,`performedAt`,`soilMoistureNote`,`userNote`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CareLogEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getPlantId());
        final String _tmp = __converters.careTypeToString(entity.getTaskType());
        if (_tmp == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, _tmp);
        }
        final Long _tmp_1 = __converters.instantToLong(entity.getPerformedAt());
        if (_tmp_1 == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, _tmp_1);
        }
        final String _tmp_2 = __converters.soilToString(entity.getSoilMoistureNote());
        if (_tmp_2 == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, _tmp_2);
        }
        if (entity.getUserNote() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getUserNote());
        }
      }
    };
  }

  @Override
  public Object insert(final CareLogEntity log, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCareLogEntity.insert(log);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object findRecent(final String plantId, final Instant since,
      final Continuation<? super List<CareLogEntity>> $completion) {
    final String _sql = "SELECT * FROM care_log WHERE plantId = ? AND performedAt >= ? ORDER BY performedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, plantId);
    _argIndex = 2;
    final Long _tmp = __converters.instantToLong(since);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, _tmp);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CareLogEntity>>() {
      @Override
      @NonNull
      public List<CareLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlantId = CursorUtil.getColumnIndexOrThrow(_cursor, "plantId");
          final int _cursorIndexOfTaskType = CursorUtil.getColumnIndexOrThrow(_cursor, "taskType");
          final int _cursorIndexOfPerformedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "performedAt");
          final int _cursorIndexOfSoilMoistureNote = CursorUtil.getColumnIndexOrThrow(_cursor, "soilMoistureNote");
          final int _cursorIndexOfUserNote = CursorUtil.getColumnIndexOrThrow(_cursor, "userNote");
          final List<CareLogEntity> _result = new ArrayList<CareLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CareLogEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPlantId;
            _tmpPlantId = _cursor.getString(_cursorIndexOfPlantId);
            final CareTaskType _tmpTaskType;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfTaskType)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfTaskType);
            }
            final CareTaskType _tmp_2 = __converters.stringToCareType(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'com.floracare.app.domain.model.CareTaskType', but it was NULL.");
            } else {
              _tmpTaskType = _tmp_2;
            }
            final Instant _tmpPerformedAt;
            final Long _tmp_3;
            if (_cursor.isNull(_cursorIndexOfPerformedAt)) {
              _tmp_3 = null;
            } else {
              _tmp_3 = _cursor.getLong(_cursorIndexOfPerformedAt);
            }
            final Instant _tmp_4 = __converters.longToInstant(_tmp_3);
            if (_tmp_4 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.Instant', but it was NULL.");
            } else {
              _tmpPerformedAt = _tmp_4;
            }
            final SoilMoistureNote _tmpSoilMoistureNote;
            final String _tmp_5;
            if (_cursor.isNull(_cursorIndexOfSoilMoistureNote)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getString(_cursorIndexOfSoilMoistureNote);
            }
            _tmpSoilMoistureNote = __converters.stringToSoil(_tmp_5);
            final String _tmpUserNote;
            if (_cursor.isNull(_cursorIndexOfUserNote)) {
              _tmpUserNote = null;
            } else {
              _tmpUserNote = _cursor.getString(_cursorIndexOfUserNote);
            }
            _item = new CareLogEntity(_tmpId,_tmpPlantId,_tmpTaskType,_tmpPerformedAt,_tmpSoilMoistureNote,_tmpUserNote);
            _result.add(_item);
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
