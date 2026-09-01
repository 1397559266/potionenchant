#version 150

#moj_import <fog.glsl>

uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform vec4 ColorModulator;

uniform float time;
uniform float opacity;
uniform vec3 tint;
uniform int tintMode;
uniform int animMode;
uniform float animSpeed;

// 贴图分辨率（体素网格），用于动画量化；diexv_sword.png 为 16x16
const float voxelGrid = 16.0;

in float vertexDistance;
in vec4 vertexColor;
in vec3 fPos;

out vec4 fragColor;

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec3 base = vertexColor.rgb;
    float a = vertexColor.a * opacity;

    // ===== 颜色控制 =====
    // tintMode: 0=贴图采样色×tint  1=纯tint色  2=动态渐变(全局色相循环)
    if (tintMode == 1) {
        base = tint;
    } else if (tintMode == 2) {
        float h = fract(time * animSpeed * 0.05);
        base = hsv2rgb(vec3(h, 0.8, 1.0)) * mix(vec3(1.0), base, 0.25);
    } else {
        base *= tint;
    }

    // ===== 动画控制（全部全局同步，平滑无体素间差异） =====
    // animMode: 0=静态 1=能量脉动(整体呼吸) 2=扫描溶解 3=波浪位移 4=彩虹循环
    if (animMode == 1) {
        float pulse = 0.7 + 0.3 * sin(time * animSpeed * 2.0);
        a *= pulse;
        base *= 0.8 + 0.2 * pulse;
    } else if (animMode == 2) {
        float scan = fract(time * animSpeed * 0.3);
        float y01 = fPos.y;
        float d = abs(y01 - scan);
        float fade = smoothstep(0.25, 0.0, d);
        a *= mix(1.0, 0.15, fade);
        base *= mix(1.0, 0.2, fade);
    } else if (animMode == 3) {
        float wave = sin(time * animSpeed + fPos.x * 0.9 + fPos.z * 0.6) * 0.5 + 0.5;
        base *= 0.85 + 0.3 * wave;
    } else if (animMode == 4) {
        float h = fract(time * animSpeed * 0.05);
        base = hsv2rgb(vec3(h, 0.9, 1.0));
    }

    fragColor = linear_fog(vec4(base, a) * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}
