package com.floracare.app.data.local;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
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
public final class DiagnosisDao_Impl implements DiagnosisDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<DiagnosisResultEntity> __insertionAdapterOfDiagnosisResultEntity;

  private final Converters __converters = new Converters();

  public DiagnosisDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfDiagnosisResultEntity = new EntityInsertionAdapter<DiagnosisResultEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `diagnosis_result` (`id`,`plantId`,`photoUri`,`diagnosisLabel`,`confidence`,`treatmentSuggestion`,`createdAt`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final DiagnosisResultEntity entity) {
        statement.bindString(1, entity.getId());
        if (entity.getPlantId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getPlantId());
        }
        statement.bindString(3, entity.getPhotoUri());
        statement.bindString(4, entity.getDiagnosisLabel());
        statement.bindDouble(5, entity.getConfidence());
        statement.bindString(6, entity.getTreatmentSuggestion());
        final Long _tmp = __converters.instantToLong(entity.getCreatedAt());
        if (_tmp == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, _tmp);
        }
      }
    };
  }

  @Override
  public Object insert(final DiagnosisResultEntity result,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfDiagnosisResultEntity.insert(result);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<DiagnosisResultEntity>> observeForPlant(final String plantId) {
    final String _sql = "SELECT * FROM diagnosis_result WHERE plantId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, plantId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"diagnosis_result"}, new Callable<List<DiagnosisResultEntity>>() {
      @Override
      @NonNull
      public List<DiagnosisResultEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfPlantId = CursorUtil.getColumnIndexOrThrow(_cursor, "plantId");
          final int _cursorIndexOfPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "photoUri");
          final int _cursorIndexOfDiagnosisLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "diagnosisLabel");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfTreatmentSuggestion = CursorUtil.getColumnIndexOrThrow(_cursor, "treatmentSuggestion");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<DiagnosisResultEntity> _result = new ArrayList<DiagnosisResultEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DiagnosisResultEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpPlantId;
            if (_cursor.isNull(_cursorIndexOfPlantId)) {
              _tmpPlantId = null;
            } else {
              _tmpPlantId = _cursor.getString(_cursorIndexOfPlantId);
            }
            final String _tmpPhotoUri;
            _tmpPhotoUri = _cursor.getString(_cursorIndexOfPhotoUri);
            final String _tmpDiagnosisLabel;
            _tmpDiagnosisLabel = _cursor.getString(_cursorIndexOfDiagnosisLabel);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final String _tmpTreatmentSuggestion;
            _tmpTreatmentSuggestion = _cursor.getString(_cursorIndexOfTreatmentSuggestion);
            final Instant _tmpCreatedAt;
            final Long _tmp;
            if (_cursor.isNull(_cursorIndexOfCreatedAt)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getLong(_cursorIndexOfCreatedAt);
            }
            final Instant _tmp_1 = __converters.longToInstant(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.Instant', but it was NULL.");
            } else {
              _tmpCreatedAt = _tmp_1;
            }
            _item = new DiagnosisResultEntity(_tmpId,_tmpPlantId,_tmpPhotoUri,_tmpDiagnosisLabel,_tmpConfidence,_tmpTreatmentSuggestion,_tmpCreatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
