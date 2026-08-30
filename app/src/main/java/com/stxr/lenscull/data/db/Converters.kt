package com.stxr.lenscull.data.db

import androidx.room.TypeConverter
import com.stxr.lenscull.domain.CullFlag
import com.stxr.lenscull.domain.PhotoFormat
import com.stxr.lenscull.domain.PreviewState
import com.stxr.lenscull.domain.RatingSyncState

class Converters {
  @TypeConverter fun format(value: PhotoFormat): String = value.name
  @TypeConverter fun format(value: String): PhotoFormat = PhotoFormat.valueOf(value)
  @TypeConverter fun flag(value: CullFlag): String = value.name
  @TypeConverter fun flag(value: String): CullFlag = CullFlag.valueOf(value)
  @TypeConverter fun syncState(value: RatingSyncState): String = value.name
  @TypeConverter fun syncState(value: String): RatingSyncState = RatingSyncState.valueOf(value)
  @TypeConverter fun previewState(value: PreviewState): String = value.name
  @TypeConverter fun previewState(value: String): PreviewState = PreviewState.valueOf(value)
}
