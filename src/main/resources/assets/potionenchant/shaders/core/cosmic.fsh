#version 150

#define M_PI 3.1415926535897932384626433832795

#moj_import <fog.glsl>

const int cosmiccount = 25;
const int cosmicoutof = 253;
const float lightmix = 0.2f;

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

uniform float time;

uniform float yaw;
uniform float pitch;
uniform float externalScale;

uniform float opacity;

uniform mat2 cosmicuvs[cosmiccount];

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec4 normal;
in vec3 fPos;

out vec4 fragColor;

mat4 rotationMatrix(vec3 axis, float angle)
{
    axis = normalize(axis);
    float s = sin(angle);
    float c = cos(angle);
    float oc = 1.0 - c;

    return mat4(oc * axis.x * axis.x + c,           oc * axis.x * axis.y - axis.z * s,  oc * axis.z * axis.x + axis.y * s,  0.0,
    oc * axis.x * axis.y + axis.z * s,  oc * axis.y * axis.y + c,           oc * axis.y * axis.z - axis.x * s,  0.0,
    oc * axis.z * axis.x - axis.y * s,  oc * axis.y * axis.z + axis.x * s,  oc * axis.z * axis.z + c,           0.0,
    0.0,                                0.0,                                0.0,                                1.0);
}

vec3 getNebulaColor(float seed, float time) {
    // 更深的浅蓝色调
    return vec3(0.45, 0.65, 0.85); // 深一些的浅蓝色
}

void main (void)
{
    vec4 mask = texture(Sampler0, texCoord0.xy);

    float oneOverExternalScale = 1.0/externalScale;

    int uvtiles = 16;
    
    float depth = length(fPos) / 10.0;

    // 固定蓝色背景
    vec3 colRGB = vec3(0.5, 0.7, 1.0);
    
    vec4 col = vec4(colRGB, 1.0); // 设置初始alpha为1.0

    // 移除光晕效果
    // 移除条纹效果

    vec4 dir = normalize(vec4(-fPos, 0));

    float sb = sin(pitch);
    float cb = cos(pitch);
    dir = normalize(vec4(dir.x, dir.y * cb - dir.z * sb, dir.y * sb + dir.z * cb, 0));

    float sa = sin(-yaw);
    float ca = cos(-yaw);
    dir = normalize(vec4(dir.z * sa + dir.x * ca, dir.y, dir.z * ca - dir.x * sa, 0));

    vec4 ray;

    for (int i = 0; i < 16; i++) {
        int mult = 16 - i;

        int j = i + 7;
        float rand1 = (j * j * 4321 + j * 8) * 2.0F;
        int k = j + 1;
        float rand2 = (k * k * k * 239 + k * 37) * 3.6F;
        float rand3 = rand1 * 347.4 + rand2 * 63.4;

        vec3 axis = normalize(vec3(sin(rand1), sin(rand2), cos(rand3)));

        ray = dir * rotationMatrix(axis, mod(rand3, 2 * M_PI));

        float rawu = 0.5 + (atan(ray.z, ray.x) / (2 * M_PI));
        float rawv = 0.5 + (asin(ray.y) / M_PI);

        float scale = mult * 0.5 + 2.75;
        float u = rawu * scale * externalScale;
        float v = (rawv + time * 0.05 * oneOverExternalScale) * scale * 0.6 * externalScale;

        vec2 tex = vec2(u, v);

        int tu = int(mod(floor(u * uvtiles), uvtiles));
        int tv = int(mod(floor(v * uvtiles), uvtiles));

        int position = ((171 * tu) + (489 * tv) + (303 * (i + 31)) + 17209) ^ 10;
        int symbol = int(mod(position, cosmicoutof));
        int rotation = int(mod(pow(tu, float(tv)) + tu + 3 + tv * i, 8));
        bool flip = false;
        if (rotation >= 4) {
            rotation -= 4;
            flip = true;
        }

        if (symbol >= 0 && symbol < cosmiccount) {

            vec2 cosmictex = vec2(1.0, 1.0);
            vec4 tcol = vec4(1.0, 0.0, 0.0, 1.0);

            float ru = clamp(mod(u, 1.0) * uvtiles - tu, 0.0, 1.0);
            float rv = clamp(mod(v, 1.0) * uvtiles - tv, 0.0, 1.0);

            if (flip) {
                ru = 1.0 - ru;
            }

            float oru = ru;
            float orv = rv;

            if (rotation == 1) {
                oru = 1.0 - rv;
                orv = ru;
            } else if (rotation == 2) {
                oru = 1.0 - ru;
                orv = 1.0 - rv;
            } else if (rotation == 3) {
                oru = rv;
                orv = 1.0 - ru;
            }

            float umin = cosmicuvs[symbol][0][0];
            float umax = cosmicuvs[symbol][1][0];
            float vmin = cosmicuvs[symbol][0][1];
            float vmax = cosmicuvs[symbol][1][1];

            cosmictex.x = umin * (1.0 - oru) + umax * oru;
            cosmictex.y = vmin * (1.0 - orv) + vmax * orv;

            tcol = texture(Sampler0, cosmictex);

            // alpha计算（星体更明显）
            float a = tcol.r * (0.8 + (1.0 / mult) * 1.2) * (1.0 - smoothstep(0.15, 0.48, abs(rawv - 0.5))) * 1.5;

            float starType = mod(rand1 * rand2, 100.0);
            vec3 starColor;

            // 修改星体颜色为静态的粉色、浅粉色、紫色、浅紫色、蓝色、浅蓝色
            if (starType < 16.6) {
                starColor = vec3(1.00, 0.40, 0.70); // 鲜艳粉色
            } else if (starType < 33.2) {
                starColor = vec3(1.00, 0.65, 0.80); // 鲜艳浅粉色
            } else if (starType < 49.8) {
                starColor = vec3(0.85, 0.20, 0.95); // 鲜艳紫色
            } else if (starType < 66.4) {
                starColor = vec3(0.90, 0.50, 1.00); // 鲜艳浅紫色
            } else if (starType < 83.0) {
                starColor = vec3(0.20, 0.40, 1.00); // 鲜艳蓝色
            } else {
                starColor = vec3(0.40, 0.65, 1.00); // 鲜艳浅蓝色
            }

            // 移除随机微调
            // starColor *= vec3(...);

            // 移除闪烁效果
            // float twinkle = ...;
            // starColor *= twinkle;

            float distanceFade = 1.0 - float(i) / 20.0;

            // 移除深度辉光
            // float depthGlow = ...;
            // vec3 glowColor = ...;
            // starColor = starColor * distanceFade * depthGlow + glowColor;

            // 简单的距离淡出
            starColor = starColor * distanceFade;

            // 移除距离辉光
            // vec3 distanceGlow = ...;
            // starColor += distanceGlow;

            col = col + vec4(starColor, 1.0) * a;
        }
    }

    vec3 lightTint = vec3(0.9, 0.9, 0.9);
    vec3 shade = vertexColor.rgb * (lightmix) + lightTint * (1.0 - lightmix);
    col.rgb *= shade;

    col.a *= mask.r * opacity;

    // 移除最终色调动态效果
    // float finalTint = ...;
    // col.rgb *= vec3(...);

    col = clamp(col, 0.0, 1.0);

    fragColor = linear_fog(col * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}