package com.floracare.app.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.floracare.app.domain.model.CareTaskSource;
import com.floracare.app.domain.model.CareTaskType;
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
import kotlinx.coroutines.flow.Flow;
import kotlinx.datetime.Instant;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CareTaskDao_Impl implements CareTaskDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CareTaskEntity> __insertionAdapterOfCareTaskEntity;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfMarkCompleted;

  private final SharedSQLiteStatement __preparedStmtOfSnooze;

  public CareTaskDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCareTaskEntity = new EntityInsertionAdapter<CareTaskEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `care_task` (`id`,`plantId`,`type`,`scheduledAt`,`completedAt`,`snoozedUntil`,`source`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CareTaskEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getPlantId());
        final String _tmp = __converters.careTypeToString(entity.getType());
        if (_tmp == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, _tmp);
        }
        final Long _tmp_1 = __converters.instantToLong(entity.getScheduledAt());
        if (_tmp_1 == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, _tmp_1);
        }
        final Long _tmp_2 = __converters.instantToLong(entity.getCompletedAt());
        if (_tmp_2 == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, _tmp_2);
        }
        final Long _tmp_3 = __converters.instantToLong(entity.getSnoozedUntil());
        if (_tmp_3 == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, _tmp_3);
        }
        final String _tmp_4 = __converters.sourceToString(entity.getSource());
        if (_tmp_4 == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, _tmp_4);
        }
      }
    };
    this.__preparedStmtOfMarkCompleted = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE care_task SET completedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSnooze = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE care_task SET snoozedUntil = ? WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsert(final CareTaskEntity task, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCareTaskEntity.insert(task);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markCompleted(final String id, final Instant at,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkCompleted.acquire();
        int _argIndex = 1;
        final Long _tmp = __converters.instantToLong(at);
        if (_tmp == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, _tmp);
        }
        _argIndex = 2;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkCompleted.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object snooze(final String id, final Instant until,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSnooze.acquire();
        int _argIndex = 1;
        final Long _tmp = __converters.instantToLong(until);
        if (_tmp == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindLong(_argIndex, _tmp);
        }
        _argIndex = 2;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSnooze.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CareTaskEntity>> observeOpenForPlant(final String plantId) {
    final String _sql = "SELECT * FROM care_task WHERE plantId = ? AND completedAt IS NULL ORDER BY scheduledAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, plantId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"care_task"}, new Callable<List<CareTaskEntity>>() {
      @Override
      @NonNull
      public List<CareTaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlantId = CursorUtil.getColumnIndexOrThrow(_cursor, "plantId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfScheduledAt = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfSnoozedUntil = CursorUtil.getColumnIndexOrThrow(_cursor, "snoozedUntil");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final List<CareTaskEntity> _result = new ArrayList<CareTaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CareTaskEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPlantId;
            _tmpPlantId = _cursor.getString(_cursorIndexOfPlantId);
            final CareTaskType _tmpType;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfType)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfType);
            }
            final CareTaskType _tmp_1 = __converters.stringToCareType(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'com.floracare.app.domain.model.CareTaskType', but it was NULL.");
            } else {
              _tmpType = _tmp_1;
            }
            final Instant _tmpScheduledAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfScheduledAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfScheduledAt);
            }
            final Instant _tmp_3 = __converters.longToInstant(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.Instant', but it was NULL.");
            } else {
              _tmpScheduledAt = _tmp_3;
            }
            final Instant _tmpCompletedAt;
            final Long _tmp_4;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmp_4 = null;
            } else {
              _tmp_4 = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _tmpCompletedAt = __converters.longToInstant(_tmp_4);
            final Instant _tmpSnoozedUntil;
            final Long _tmp_5;
            if (_cursor.isNull(_cursorIndexOfSnoozedUntil)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getLong(_cursorIndexOfSnoozedUntil);
            }
            _tmpSnoozedUntil = __converters.longToInstant(_tmp_5);
            final CareTaskSource _tmpSource;
            final String _tmp_6;
            if (_cursor.isNull(_cursorIndexOfSource)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getString(_cursorIndexOfSource);
            }
            final CareTaskSource _tmp_7 = __converters.stringToSource(_tmp_6);
            if (_tmp_7 == null) {
              throw new IllegalStateException("Expected NON-NULL 'com.floracare.app.domain.model.CareTaskSource', but it was NULL.");
            } else {
              _tmpSource = _tmp_7;
            }
            _item = new CareTaskEntity(_tmpId,_tmpPlantId,_tmpType,_tmpScheduledAt,_tmpCompletedAt,_tmpSnoozedUntil,_tmpSource);
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
  public Object findDueBefore(final Instant until,
      final Continuation<? super List<CareTaskEntity>> $completion) {
    final String _sql = "SELECT * FROM care_task WHERE completedAt IS NULL AND scheduledAt <= ? ORDER BY scheduledAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    final Long _tmp = __converters.instantToLong(until);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, _tmp);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<CareTaskEntity>>() {
      @Override
      @NonNull
      public List<CareTaskEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlantId = CursorUtil.getColumnIndexOrThrow(_cursor, "plantId");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfScheduledAt = CursorUtil.getColumnIndexOrThrow(_cursor, "scheduledAt");
          final int _cursorIndexOfCompletedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "completedAt");
          final int _cursorIndexOfSnoozedUntil = CursorUtil.getColumnIndexOrThrow(_cursor, "snoozedUntil");
          final int _cursorIndexOfSource = CursorUtil.getColumnIndexOrThrow(_cursor, "source");
          final List<CareTaskEntity> _result = new ArrayList<CareTaskEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CareTaskEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPlantId;
            _tmpPlantId = _cursor.getString(_cursorIndexOfPlantId);
            final CareTaskType _tmpType;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfType)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfType);
            }
            final CareTaskType _tmp_2 = __converters.stringToCareType(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'com.floracare.app.domain.model.CareTaskType', but it was NULL.");
            } else {
              _tmpType = _tmp_2;
            }
            final Instant _tmpScheduledAt;
            final Long _tmp_3;
            if (_cursor.isNull(_cursorIndexOfScheduledAt)) {
              _tmp_3 = null;
            } else {
              _tmp_3 = _cursor.getLong(_cursorIndexOfScheduledAt);
            }
            final Instant _tmp_4 = __converters.longToInstant(_tmp_3);
            if (_tmp_4 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.Instant', but it was NULL.");
            } else {
              _tmpScheduledAt = _tmp_4;
            }
            final Instant _tmpCompletedAt;
            final Long _tmp_5;
            if (_cursor.isNull(_cursorIndexOfCompletedAt)) {
              _tmp_5 = null;
            } else {
              _tmp_5 = _cursor.getLong(_cursorIndexOfCompletedAt);
            }
            _tmpCompletedAt = __converters.longToInstant(_tmp_5);
            final Instant _tmpSnoozedUntil;
            final Long _tmp_6;
            if (_cursor.isNull(_cursorIndexOfSnoozedUntil)) {
              _tmp_6 = null;
            } else {
              _tmp_6 = _cursor.getLong(_cursorIndexOfSnoozedUntil);
            }
            _tmpSnoozedUntil = __converters.longToInstant(_tmp_6);
            final CareTaskSource _tmpSource;
            final String _tmp_7;
            if (_cursor.isNull(_cursorIndexOfSource)) {
              _tmp_7 = null;
            } else {
              _tmp_7 = _cursor.getString(_cursorIndexOfSource);
            }
            final CareTaskSource _tmp_8 = __converters.stringToSource(_tmp_7);
            if (_tmp_8 == null) {
              throw new IllegalStateException("Expected NON-NULL 'com.floracare.app.domain.model.CareTaskSource', but it was NULL.");
            } else {
              _tmpSource = _tmp_8;
            }
            _item = new CareTaskEntity(_tmpId,_tmpPlantId,_tmpType,_tmpScheduledAt,_tmpCompletedAt,_tmpSnoozedUntil,_tmpSource);
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
