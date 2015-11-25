#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Skinning.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"

attribute vec3 inPosition;
uniform float m_ImpositorAlpha;

#if defined(HAS_COLORMAP) || (defined(HAS_LIGHTMAP) && !defined(SEPARATE_TEXCOORD))
    #define NEED_TEXCOORD1
#endif

attribute vec2 inTexCoord;
attribute vec2 inTexCoord2;
attribute vec4 inColor;
attribute vec3 inNormal;

varying vec2 texCoord1;
varying vec2 texCoord2;

varying vec4 vertColor;
varying float impositorAlpha;

void main(){
    #ifdef NEED_TEXCOORD1
        texCoord1 = inTexCoord;
    #endif

    #ifdef SEPARATE_TEXCOORD
        texCoord2 = inTexCoord2;
    #endif

    #ifdef HAS_VERTEXCOLOR
        vertColor = inColor;
    #endif

    vec4 modelSpacePos = vec4(inPosition, 1.0);
    #ifdef NUM_BONES
        Skinning_Compute(modelSpacePos);
    #endif

	/*
	vec3 wvPosition = TransformWorldView(modelSpacePos).xyz;// (g_WorldViewMatrix * modelSpacePos).xyz;
	vec3 wvNormal  = normalize(TransformNormal(inNormal));//normalize(g_NormalMatrix * modelSpaceNorm);
	vec3 viewDir = normalize(-wvPosition);
	impositorAlpha = max(0.0, dot(wvNormal, viewDir));
	impositorAlpha = smoothstep(0, 1, impositorAlpha);
	*/
	//impositorAlpha = m_ImpositorAlpha;
	vec3 wvPosition = TransformWorldView(modelSpacePos).xyz;// (g_WorldViewMatrix * modelSpacePos).xyz;
	float dist = length(wvPosition);
	impositorAlpha = clamp((dist-30.0)/20.0, 0.0, 1.0);

    gl_Position = TransformWorldViewProjection(modelSpacePos);
}