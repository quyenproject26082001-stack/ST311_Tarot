package com.aaa.bbb.ccc.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.aaa.bbb.ccc.data.local.entity.CharacterColorEntity
import com.aaa.bbb.ccc.data.local.entity.CharacterEntity
import com.aaa.bbb.ccc.data.local.entity.CharacterLayerEntity
import com.aaa.bbb.ccc.data.local.entity.CharacterLayerGroupEntity

data class LayerWithColors(
    @Embedded val layer: CharacterLayerEntity,
    @Relation(parentColumn = "id", entityColumn = "layerId")
    val colors: List<CharacterColorEntity>
)

data class LayerGroupWithLayers(
    @Embedded val layerGroup: CharacterLayerGroupEntity,
    @Relation(
        entity = CharacterLayerEntity::class,
        parentColumn = "id",
        entityColumn = "layerGroupId"
    )
    val layers: List<LayerWithColors>
)

data class CharacterWithLayers(
    @Embedded val character: CharacterEntity,
    @Relation(
        entity = CharacterLayerGroupEntity::class,
        parentColumn = "id",
        entityColumn = "characterId"
    )
    val layerGroups: List<LayerGroupWithLayers>
)
