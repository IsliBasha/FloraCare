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
public final class WeatherDao_Impl implements WeatherDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WeatherSnapshotEntity> __insertionAdapterOfWeatherSnapshotEntity;

  private final Converters __converters = new Converters();

  public WeatherDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWeatherSnapshotEntity = new EntityInsertionAdapter<WeatherSnapshotEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `weather_snapshot` (`id`,`lat`,`lon`,`recordedAt`,`tempC`,`humidityPct`,`rainMm`,`uvIndex`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WeatherSnapshotEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindDouble(2, entity.getLat());
        statement.bindDouble(3, entity.getLon());
        final Long _tmp = __converters.instantToLong(entity.getRecordedAt());
        if (_tmp == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, _tmp);
        }
        statement.bindDouble(5, entity.getTempC());
        statement.bindDouble(6, entity.getHumidityPct());
        statement.bindDouble(7, entity.getRainMm());
        statement.bindDouble(8, entity.getUvIndex());
      }
    };
  }

  @Override
  public Object insert(final WeatherSnapshotEntity snapshot,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWeatherSnapshotEntity.insert(snapshot);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object findRecent(final Instant since,
      final Continuation<? super List<WeatherSnapshotEntity>> $completion) {
    final String _sql = "SELECT * FROM weather_snapshot WHERE recordedAt >= ? ORDER BY recordedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    final Long _tmp = __converters.instantToLong(since);
    if (_tmp == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindLong(_argIndex, _tmp);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<WeatherSnapshotEntity>>() {
      @Override
      @NonNull
      public List<WeatherSnapshotEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLat = CursorUtil.getColumnIndexOrThrow(_cursor, "lat");
          final int _cursorIndexOfLon = CursorUtil.getColumnIndexOrThrow(_cursor, "lon");
          final int _cursorIndexOfRecordedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "recordedAt");
          final int _cursorIndexOfTempC = CursorUtil.getColumnIndexOrThrow(_cursor, "tempC");
          final int _cursorIndexOfHumidityPct = CursorUtil.getColumnIndexOrThrow(_cursor, "humidityPct");
          final int _cursorIndexOfRainMm = CursorUtil.getColumnIndexOrThrow(_cursor, "rainMm");
          final int _cursorIndexOfUvIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "uvIndex");
          final List<WeatherSnapshotEntity> _result = new ArrayList<WeatherSnapshotEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WeatherSnapshotEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final double _tmpLat;
            _tmpLat = _cursor.getDouble(_cursorIndexOfLat);
            final double _tmpLon;
            _tmpLon = _cursor.getDouble(_cursorIndexOfLon);
            final Instant _tmpRecordedAt;
            final Long _tmp_1;
            if (_cursor.isNull(_cursorIndexOfRecordedAt)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getLong(_cursorIndexOfRecordedAt);
            }
            final Instant _tmp_2 = __converters.longToInstant(_tmp_1);
            if (_tmp_2 == null) {
              throw new IllegalStateException("Expected NON-NULL 'kotlinx.datetime.Instant', but it was NULL.");
            } else {
              _tmpRecordedAt = _tmp_2;
            }
            final float _tmpTempC;
            _tmpTempC = _cursor.getFloat(_cursorIndexOfTempC);
            final float _tmpHumidityPct;
            _tmpHumidityPct = _cursor.getFloat(_cursorIndexOfHumidityPct);
            final float _tmpRainMm;
            _tmpRainMm = _cursor.getFloat(_cursorIndexOfRainMm);
            final float _tmpUvIndex;
            _tmpUvIndex = _cursor.getFloat(_cursorIndexOfUvIndex);
            _item = new WeatherSnapshotEntity(_tmpId,_tmpLat,_tmpLon,_tmpRecordedAt,_tmpTempC,_tmpHumidityPct,_tmpRainMm,_tmpUvIndex);
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
