package com.floracare.app.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.floracare.app.domain.model.LocationTag;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalStateException;
import java.lang.Integer;
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
public final class PlantDao_Impl implements PlantDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PlantEntity> __insertionAdapterOfPlantEntity;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<PlantEntity> __updateAdapterOfPlantEntity;

  public PlantDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPlantEntity = new EntityInsertionAdapter<PlantEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `plant` (`id`,`nickname`,`speciesId`,`locationTag`,`acquiredAt`,`coverPhotoUri`,`notes`,`archived`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlantEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getNickname());
        if (entity.getSpeciesId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getSpeciesId());
        }
        final String _tmp = __converters.locationToString(entity.getLocationTag());
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, _tmp);
        }
        final Long _tmp_1 = __converters.instantToLong(entity.getAcquiredAt());
        if (_tmp_1 == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, _tmp_1);
        }
        if (entity.getCoverPhotoUri() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getCoverPhotoUri());
        }
        statement.bindString(7, entity.getNotes());
        final int _tmp_2 = entity.getArchived() ? 1 : 0;
        statement.bindLong(8, _tmp_2);
      }
    };
    this.__updateAdapterOfPlantEntity = new EntityDeletionOrUpdateAdapter<PlantEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `plant` SET `id` = ?,`nickname` = ?,`speciesId` = ?,`locationTag` = ?,`acquiredAt` = ?,`coverPhotoUri` = ?,`notes` = ?,`archived` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PlantEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getNickname());
        if (entity.getSpeciesId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getSpeciesId());
        }
        final String _tmp = __converters.locationToString(entity.getLocationTag());
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, _tmp);
        }
        final Long _tmp_1 = __converters.instantToLong(entity.getAcquiredAt());
        if (_tmp_1 == null) {
          statement.bindNull(5);
        } else {
          statement.bindLong(5, _tmp_1);
        }
        if (entity.getCoverPhotoUri() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getCoverPhotoUri());
        }
        statement.bindString(7, entity.getNotes());
        final int _tmp_2 = entity.getArchived() ? 1 : 0;
        statement.bindLong(8, _tmp_2);
        statement.bindString(9, entity.getId());
      }
    };
  }

  @Override
  public Object upsert(final PlantEntity plant, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPlantEntity.insert(plant);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final PlantEntity plant, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfPlantEntity.handle(plant);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<PlantEntity>> observeActive() {
    final String _sql = "SELECT * FROM plant WHERE archived = 0 ORDER BY nickname ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"plant"}, new Callable<List<PlantEntity>>() {
      @Override
      @NonNull
      public List<PlantEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "nickname");
          final int _cursorIndexOfSpeciesId = CursorUtil.getColumnIndexOrThrow(_cursor, "speciesId");
          final int _cursorIndexOfLocationTag = CursorUtil.getColumnIndexOrThrow(_cursor, "locationTag");
          final int _cursorIndexOfAcquiredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "acquiredAt");
          final int _cursorIndexOfCoverPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "coverPhotoUri");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "archived");
          final List<PlantEntity> _result = new ArrayList<PlantEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PlantEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpNickname;
            _tmpNickname = _cursor.getString(_cursorIndexOfNickname);
            final String _tmpSpeciesId;
            if (_cursor.isNull(_cursorIndexOfSpeciesId)) {
              _tmpSpeciesId = null;
            } else {
              _tmpSpeciesId = _cursor.getString(_cursorIndexOfSpeciesId);
            }
            final LocationTag _tmpLocationTag;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfLocationTag)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfLocationTag);
            }
            final LocationTag _tmp_1 = __converters.stringToLocation(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'com.floracare.app.domain.model.LocationTag', but it was NULL.");
            } else {
              _tmpLocationTag = _tmp_1;
            }
            final Instant _tmpAcquiredAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfAcquiredAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfAcquiredAt);
            }
            final Instant _tmp_3 = __converters.longToInstant(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.Instant', but it was NULL.");
            } else {
              _tmpAcquiredAt = _tmp_3;
            }
            final String _tmpCoverPhotoUri;
            if (_cursor.isNull(_cursorIndexOfCoverPhotoUri)) {
              _tmpCoverPhotoUri = null;
            } else {
              _tmpCoverPhotoUri = _cursor.getString(_cursorIndexOfCoverPhotoUri);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final boolean _tmpArchived;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfArchived);
            _tmpArchived = _tmp_4 != 0;
            _item = new PlantEntity(_tmpId,_tmpNickname,_tmpSpeciesId,_tmpLocationTag,_tmpAcquiredAt,_tmpCoverPhotoUri,_tmpNotes,_tmpArchived);
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
  public Object findById(final String id, final Continuation<? super PlantEntity> $completion) {
    final String _sql = "SELECT * FROM plant WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PlantEntity>() {
      @Override
      @Nullable
      public PlantEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNickname = CursorUtil.getColumnIndexOrThrow(_cursor, "nickname");
          final int _cursorIndexOfSpeciesId = CursorUtil.getColumnIndexOrThrow(_cursor, "speciesId");
          final int _cursorIndexOfLocationTag = CursorUtil.getColumnIndexOrThrow(_cursor, "locationTag");
          final int _cursorIndexOfAcquiredAt = CursorUtil.getColumnIndexOrThrow(_cursor, "acquiredAt");
          final int _cursorIndexOfCoverPhotoUri = CursorUtil.getColumnIndexOrThrow(_cursor, "coverPhotoUri");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final int _cursorIndexOfArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "archived");
          final PlantEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpNickname;
            _tmpNickname = _cursor.getString(_cursorIndexOfNickname);
            final String _tmpSpeciesId;
            if (_cursor.isNull(_cursorIndexOfSpeciesId)) {
              _tmpSpeciesId = null;
            } else {
              _tmpSpeciesId = _cursor.getString(_cursorIndexOfSpeciesId);
            }
            final LocationTag _tmpLocationTag;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfLocationTag)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfLocationTag);
            }
            final LocationTag _tmp_1 = __converters.stringToLocation(_tmp);
            if (_tmp_1 == null) {
              throw new IllegalStateException("Expected NON-NULL 'com.floracare.app.domain.model.LocationTag', but it was NULL.");
            } else {
              _tmpLocationTag = _tmp_1;
            }
            final Instant _tmpAcquiredAt;
            final Long _tmp_2;
            if (_cursor.isNull(_cursorIndexOfAcquiredAt)) {
              _tmp_2 = null;
            } else {
              _tmp_2 = _cursor.getLong(_cursorIndexOfAcquiredAt);
            }
            final Instant _tmp_3 = __converters.longToInstant(_tmp_2);
            if (_tmp_3 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.Instant', but it was NULL.");
            } else {
              _tmpAcquiredAt = _tmp_3;
            }
            final String _tmpCoverPhotoUri;
            if (_cursor.isNull(_cursorIndexOfCoverPhotoUri)) {
              _tmpCoverPhotoUri = null;
            } else {
              _tmpCoverPhotoUri = _cursor.getString(_cursorIndexOfCoverPhotoUri);
            }
            final String _tmpNotes;
            _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            final boolean _tmpArchived;
            final int _tmp_4;
            _tmp_4 = _cursor.getInt(_cursorIndexOfArchived);
            _tmpArchived = _tmp_4 != 0;
            _result = new PlantEntity(_tmpId,_tmpNickname,_tmpSpeciesId,_tmpLocationTag,_tmpAcquiredAt,_tmpCoverPhotoUri,_tmpNotes,_tmpArchived);
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
  public Object count(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM plant";
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
