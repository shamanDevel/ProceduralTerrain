/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.shaman.terrain.vegetation;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;

/**
 * <code>Quad</code> represents a rectangular plane in space
 * defined by 4 vertices. The quad's lower-left side is contained
 * at the local space origin (-width/2, 0, 0), while the upper-right
 * side is located at the width/height coordinates (width/2, 0, height).
 * 
 * @author Kirill Vainer, Sebastian Weiß
 */
public class QuadXZ extends Mesh {

    private float width;
    private float height;

    /**
     * Serialization only. Do not use.
     */
    public QuadXZ(){
    }

    /**
     * Create a quad with the given width and height. The quad
     * is always created in the XZ plane.
     * 
     * @param width The X extent or width
     * @param height The Y extent or width
     */
    public QuadXZ(float width, float height){
        updateGeometry(width, height);
    }

    /**
     * Create a quad with the given width and height. The quad
     * is always created in the XZ plane.
     * 
     * @param width The X extent or width
     * @param height The Y extent or width
     * @param flipCoords If true, the texture coordinates will be flipped
     * along the Y axis.
     */
    public QuadXZ(float width, float height, boolean flipCoords){
        updateGeometry(width, height, flipCoords);
    }

    public float getHeight() {
        return height;
    }

    public float getWidth() {
        return width;
    }

    public void updateGeometry(float width, float height){
        updateGeometry(width, height, false);
    }

    public void updateGeometry(float width, float height, boolean flipCoords) {
        this.width = width;
        this.height = height;
        setBuffer(VertexBuffer.Type.Position, 3, new float[]{width/2,      0,      0,
                                                -width/2,  0,      0,
                                                -width/2,  0, height,
                                                +width/2, 0, height
                                                });
        

        if (flipCoords){
            setBuffer(VertexBuffer.Type.TexCoord, 2, new float[]{0, 1,
                                                    1, 1,
                                                    1, 0,
                                                    0, 0});
        }else{
            setBuffer(VertexBuffer.Type.TexCoord, 2, new float[]{0, 0,
                                                    1, 0,
                                                    1, 1,
                                                    0, 1});
        }
        setBuffer(VertexBuffer.Type.Normal, 3, new float[]{0, 1, 0,
                                              0, 1, 0,
                                              0, 1, 0,
                                              0, 1, 0});
        if (height < 0){
            setBuffer(VertexBuffer.Type.Index, 3, new short[]{0, 2, 1,
                                                 0, 3, 2});
        }else{
            setBuffer(VertexBuffer.Type.Index, 3, new short[]{0, 1, 2,
                                                 0, 2, 3});
        }
        
        updateBound();
    }


}

