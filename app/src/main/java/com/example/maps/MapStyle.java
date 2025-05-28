package com.example.maps;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;

public class MapStyle {
    public static boolean  changeMapStyle(boolean Mapnik, MapView map){
        //setup map style
        if(Mapnik){
            OnlineTileSourceBase tileSource = new OnlineTileSourceBase("OpenTopoMap", 0, 18, 256, ".png",
                    new String[] { "https://a.tile.opentopomap.org/" }) {
                @Override
                public String getTileURLString(long pMapTileIndex) {
                    return getBaseUrl()
                            + MapTileIndex.getZoom(pMapTileIndex) + "/"
                            + MapTileIndex.getX(pMapTileIndex) + "/"
                            + MapTileIndex.getY(pMapTileIndex) + mImageFilenameEnding;
                }
            };
            Mapnik = false;
            map.setTileSource(tileSource);
        }else{
            // Switch back to OSM Mapnik
            map.setTileSource(TileSourceFactory.MAPNIK);
            Mapnik = true;
        }
        return Mapnik;


    }
}
