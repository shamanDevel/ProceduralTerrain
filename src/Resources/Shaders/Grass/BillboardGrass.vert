/* Base shader for trees */
uniform mat4 g_WorldViewProjectionMatrix;
uniform mat4 g_WorldMatrix;
uniform vec3 g_CameraPosition;
uniform vec3 g_CameraLeft;

attribute vec3 inPosition;
attribute vec2 inTexCoord;
attribute vec2 inTexCoord2;
attribute vec4 inColor;

#ifdef FADE_ENABLED
uniform float m_FadeEnd;
uniform float m_FadeRange;
#endif

#ifdef SWAYING
uniform float g_Time;
uniform vec2 m_Wind;
uniform vec3 m_SwayData;
#endif

#ifdef VERTEX_LIGHTING
uniform vec4 g_LightPosition[4];
uniform vec4 g_LightColor[4];
uniform vec4 g_LightDirection[4];

uniform vec4 g_AmbientLightColor;

varying vec3 diffuseLight;
varying vec3 ambientLight;
#endif

#ifdef VERTEX_COLORS
varying vec4 color;
#endif
varying vec3 texCoord;

void main() { 
    texCoord = vec3(inTexCoord,1.0);
    vec4 pos = vec4(inPosition,1.0);
    #ifdef VERTEX_COLORS
    color = inColor;
    #endif

    vec4 worldPos = g_WorldMatrix*pos;

    #ifdef VERTEX_LIGHTING
    diffuseLight = vec3(0.0,0.0,0.0);
    for(int i = 0; i < NUM_LIGHTS; i++){
        float posLight = step(0.5, g_LightColor[i].w);
        vec3 lightVec = g_LightPosition[i].xyz * sign(posLight - 0.5) - (worldPos.xyz * posLight);
        float lDist = length(lightVec);

        float att = clamp(1.0 - g_LightPosition[i].w * lDist * posLight, 0.0, 1.0);
        lightVec = lightVec / vec3(lDist);
        //Spotlights
        float spotFallOff = 1.0;
        if(g_LightDirection[i].w != 0.0){
            vec3 spotdir = normalize(g_LightDirection[i].xyz);
            float curAngleCos = dot(-lightVec, spotdir);    
            float innerAngleCos = floor(g_LightDirection[i].w) * 0.001;
            float outerAngleCos = fract(g_LightDirection[i].w);
            float innerMinusOuter = innerAngleCos - outerAngleCos;
            spotFallOff = clamp((curAngleCos - outerAngleCos) / innerMinusOuter, 0.0, 1.0);
        }
        diffuseLight += g_LightColor[i].rgb*(att*spotFallOff);
        }
    ambientLight = g_AmbientLightColor.rgb;
    #endif

    #if defined(FADE_ENABLED) || defined(SWAYING)
    float dist = distance(g_CameraPosition.xz, worldPos.xz);
    #endif

    #ifdef FADE_ENABLED
    texCoord.z = clamp((m_FadeEnd - dist)/(m_FadeRange),0.0,1.0);
    #endif

    #ifdef SWAYING
    float angle = (g_Time + pos.x*m_SwayData.y) * m_SwayData.x;
    pos.xz += 0.1*m_Wind*inTexCoord.y*sin(angle);
    #endif
    
    //Billboard rotation
    vec2 cl = normalize(g_CameraLeft.xz);
    pos.xyz += vec3(-cl.x*inTexCoord2.x,inTexCoord2.y,-cl.y*inTexCoord2.x);
    
    gl_Position = g_WorldViewProjectionMatrix * pos;
}