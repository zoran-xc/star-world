package top.xcyyds.starworld.common.nation;

public record NationQueryResult(
        Nation nation,
        NationBorderBand borderBand,
        double borderDistanceBlocks,
        int bufferInnerStartBlocks,
        int bufferTotalBlocks
) {
}
