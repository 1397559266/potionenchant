#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

uniform float time;
uniform float animSpeed;
uniform int animMode;

out float vertexDistance;
out vec4 vertexColor;
out vec3 fPos;

void main() {
    vec3 pos = Position;
    if (animMode == 3) {
        // 波浪位移：沿 y 轴的正弦波，逐体素错开
        float wave = sin(time * animSpeed + pos.x * 0.9 + pos.z * 0.6) * 0.5;
        pos.y += wave;
    }
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    fPos = Position;
    vertexDistance = fog_distance(ModelViewMat, pos, FogShape);
    vertexColor = Color;
}
