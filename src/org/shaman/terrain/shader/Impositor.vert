#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"

attribute vec3 inPosition;
uniform float m_FadeNear;
uniform float m_FadeFar;

attribute vec3 inTexCoord;
attribute vec2 inTexCoord2;
attribute vec4 inColor;
attribute vec3 inNormal;

varying vec3 texCoord1;
varying vec2 texCoord2;

varying vec4 vertColor;
varying float impositorAlpha;

void main(){
    texCoord1 = inTexCoord;

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
	impositorAlpha = clamp((dist-m_FadeNear)/(m_FadeFar-m_FadeNear), 0.0, 1.0);

    gl_Position = TransformWorldViewProjection(modelSpacePos);
}