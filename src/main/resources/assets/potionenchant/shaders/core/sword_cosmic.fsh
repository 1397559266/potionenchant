#version 150

#define M_PI 3.1415926535897932384626433832795

#moj_import <fog.glsl>

const int CHAR_COUNT = 62;
const int FONT_ROWS = 7;
const int FONT_COLS = 5;

const int S1_COUNT = 140;
const int S2_COUNT = 120;
const int S3_COUNT = 80;

const int fontData[434] = int[](
    14,17,19,21,25,17,14,   4,12,4,4,4,4,14,   14,17,1,2,4,8,31,    14,17,1,6,1,17,14,
    2,6,10,18,31,2,2,      31,16,30,1,1,17,14, 14,16,16,30,17,17,14, 31,1,2,4,8,8,8,
    14,17,17,14,17,17,14,  14,17,17,15,1,2,12, 4,10,17,17,31,17,17,  30,17,17,30,17,17,30,
    14,17,16,16,16,17,14,  30,17,17,17,17,17,30,31,16,16,30,16,16,31,31,16,16,30,16,16,16,
    14,17,16,23,17,17,14,  17,17,17,31,17,17,17,14,4,4,4,4,4,14,     7,2,2,2,2,18,12,
    17,18,20,24,20,18,17,  16,16,16,16,16,16,31,17,27,21,21,17,17,17,17,17,25,21,19,17,17,
    14,17,17,17,17,17,14,  30,17,17,30,16,16,16,14,17,17,17,21,18,13,30,17,17,30,20,18,17,
    14,17,16,14,1,17,14,   31,4,4,4,4,4,4,      17,17,17,17,17,17,14, 17,17,17,17,17,10,4,
    17,17,17,21,21,27,17,  17,17,10,4,10,17,17, 17,17,10,4,4,4,4,    31,1,2,4,8,16,31,
    0,0,14,1,15,17,15,     16,16,22,25,17,17,30,0,0,14,16,16,17,14,  1,1,13,19,17,17,15,
    0,0,14,17,31,16,14,    6,9,8,28,8,8,8,      0,0,15,17,15,1,14,   16,16,22,25,17,17,17,
    4,0,12,4,4,4,14,       2,0,6,2,2,18,12,     16,16,18,20,24,20,18,12,4,4,4,4,4,14,
    0,0,26,21,21,17,17,    0,0,22,25,17,17,17,  0,0,14,17,17,17,14,  0,0,30,17,30,16,16,
    0,0,15,17,15,1,1,      0,0,22,25,16,16,16,  0,0,14,16,14,1,30,   8,8,28,8,8,9,6,
    0,0,17,17,17,19,13,    0,0,17,17,17,10,4,   0,0,17,17,21,21,10,  0,0,17,10,4,10,17,
    0,0,17,17,15,1,14,     0,0,31,2,4,8,31
);

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

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec4 normal;
in vec3 fPos;

out vec4 fragColor;

float hash(float n) {
    return fract(sin(n) * 43758.5453);
}

bool getCharPixel(int charIdx, int row, int col) {
    int bitmask = fontData[charIdx * FONT_ROWS + row];
    return ((bitmask >> (4 - col)) & 1) == 1;
}

void main(void) {
    vec4 mask = texture(Sampler0, texCoord0.xy);

    vec4 dir = normalize(vec4(-fPos, 0));
    float sb = sin(pitch), cb = cos(pitch);
    dir = normalize(vec4(dir.x, dir.y * cb - dir.z * sb, dir.y * sb + dir.z * cb, 0));
    float sa = sin(-yaw), ca = cos(-yaw);
    dir = normalize(vec4(dir.z * sa + dir.x * ca, dir.y, dir.z * ca - dir.x * sa, 0));

    float u = atan(dir.z, dir.x) / (2.0 * M_PI) + 0.5;
    float skyV = asin(clamp(dir.y, -1.0, 1.0)) / M_PI + 0.5;

    float oneOverExternalScale = 1.0 / max(externalScale, 0.1);
    float charScale = 0.18 * oneOverExternalScale;

    vec3 accumColor = vec3(0.0);
    float accumAlpha = 0.0;

    for (int i = 0; i < S1_COUNT; i++) {
        float seed = 1000.0 + float(i) * 137.0;
        float streamU = hash(seed);
        float diffU = u - streamU;
        if (diffU > 0.5) diffU -= 1.0;
        else if (diffU < -0.5) diffU += 1.0;
        float charW = 0.010 * charScale;
        if (abs(diffU) >= charW * 0.5) continue;
        float localU = diffU / charW + 0.5;
        int numChars = int(floor(hash(seed + 2.0) * 20.0 + 10.0));
        float direction = (hash(seed + 3.0) > 0.5) ? 1.0 : -1.0;
        float streamSpeed = 0.01 * (0.6 + hash(seed + 4.0) * 0.8);
        float phase = hash(seed + 5.0) * 100.0;
        float scrollV = fract(time * streamSpeed * direction + phase);
        float charHeight = charW * 1.5;
        float spacing = charHeight * 1.3;
        float halfChar = charHeight * 0.5;
        float vOffset;
        if (direction > 0.0) {
            vOffset = fract(scrollV - skyV + 1.0);
        } else {
            vOffset = fract(skyV - scrollV + 1.0);
        }
        float trailLength = float(numChars) * spacing;
        if (vOffset >= trailLength) continue;
        int ci = int(floor(vOffset / spacing));
        if (ci >= numChars) continue;
        float slotCenter = (float(ci) + 0.5) * spacing;
        float distFromCenter = abs(vOffset - slotCenter);
        if (distFromCenter >= halfChar) continue;
        int asciiIdx = int(mod(floor(seed + float(ci) * 47.0), float(CHAR_COUNT)));
        float localVchar = (vOffset - slotCenter) / halfChar * 0.5 + 0.5;
        localVchar = clamp(localVchar, 0.0, 1.0);
        int px = int(floor(localU * float(FONT_COLS)));
        int py = int(floor(localVchar * float(FONT_ROWS)));
        if (px >= 0 && px < FONT_COLS && py >= 0 && py < FONT_ROWS) {
            if (getCharPixel(asciiIdx, py, px)) {
                float a = opacity * 0.70;
                accumColor += vec3(0.15, 2.4, 2.2) * a;
                accumAlpha += a;
            }
        }
    }

    for (int i = 0; i < S2_COUNT; i++) {
        float seed = 2000.0 + float(i) * 137.0;
        float streamU = hash(seed);
        float diffU = u - streamU;
        if (diffU > 0.5) diffU -= 1.0;
        else if (diffU < -0.5) diffU += 1.0;
        float charW = 0.018 * charScale;
        if (abs(diffU) >= charW * 0.5) continue;
        float localU = diffU / charW + 0.5;
        int numChars = int(floor(hash(seed + 2.0) * 20.0 + 10.0));
        float direction = (hash(seed + 3.0) > 0.5) ? 1.0 : -1.0;
        float streamSpeed = 0.01 * (0.6 + hash(seed + 4.0) * 0.8);
        float phase = hash(seed + 5.0) * 100.0;
        float scrollV = fract(time * streamSpeed * direction + phase);
        float charHeight = charW * 1.5;
        float spacing = charHeight * 1.3;
        float halfChar = charHeight * 0.5;
        float vOffset;
        if (direction > 0.0) {
            vOffset = fract(scrollV - skyV + 1.0);
        } else {
            vOffset = fract(skyV - scrollV + 1.0);
        }
        float trailLength = float(numChars) * spacing;
        if (vOffset >= trailLength) continue;
        int ci = int(floor(vOffset / spacing));
        if (ci >= numChars) continue;
        float slotCenter = (float(ci) + 0.5) * spacing;
        float distFromCenter = abs(vOffset - slotCenter);
        if (distFromCenter >= halfChar) continue;
        int asciiIdx = int(mod(floor(seed + float(ci) * 47.0), float(CHAR_COUNT)));
        float localVchar = (vOffset - slotCenter) / halfChar * 0.5 + 0.5;
        localVchar = clamp(localVchar, 0.0, 1.0);
        int px = int(floor(localU * float(FONT_COLS)));
        int py = int(floor(localVchar * float(FONT_ROWS)));
        if (px >= 0 && px < FONT_COLS && py >= 0 && py < FONT_ROWS) {
            if (getCharPixel(asciiIdx, py, px)) {
                float a = opacity * 1.00;
                accumColor += vec3(0.15, 2.4, 2.2) * a;
                accumAlpha += a;
            }
        }
    }

    for (int i = 0; i < S3_COUNT; i++) {
        float seed = 3000.0 + float(i) * 137.0;
        float streamU = hash(seed);
        float diffU = u - streamU;
        if (diffU > 0.5) diffU -= 1.0;
        else if (diffU < -0.5) diffU += 1.0;
        float charW = 0.026 * charScale;
        if (abs(diffU) >= charW * 0.5) continue;
        float localU = diffU / charW + 0.5;
        int numChars = int(floor(hash(seed + 2.0) * 20.0 + 10.0));
        float direction = (hash(seed + 3.0) > 0.5) ? 1.0 : -1.0;
        float streamSpeed = 0.01 * (0.6 + hash(seed + 4.0) * 0.8);
        float phase = hash(seed + 5.0) * 100.0;
        float scrollV = fract(time * streamSpeed * direction + phase);
        float charHeight = charW * 1.5;
        float spacing = charHeight * 1.3;
        float halfChar = charHeight * 0.5;
        float vOffset;
        if (direction > 0.0) {
            vOffset = fract(scrollV - skyV + 1.0);
        } else {
            vOffset = fract(skyV - scrollV + 1.0);
        }
        float trailLength = float(numChars) * spacing;
        if (vOffset >= trailLength) continue;
        int ci = int(floor(vOffset / spacing));
        if (ci >= numChars) continue;
        float slotCenter = (float(ci) + 0.5) * spacing;
        float distFromCenter = abs(vOffset - slotCenter);
        if (distFromCenter >= halfChar) continue;
        int asciiIdx = int(mod(floor(seed + float(ci) * 47.0), float(CHAR_COUNT)));
        float localVchar = (vOffset - slotCenter) / halfChar * 0.5 + 0.5;
        localVchar = clamp(localVchar, 0.0, 1.0);
        int px = int(floor(localU * float(FONT_COLS)));
        int py = int(floor(localVchar * float(FONT_ROWS)));
        if (px >= 0 && px < FONT_COLS && py >= 0 && py < FONT_ROWS) {
            if (getCharPixel(asciiIdx, py, px)) {
                float a = opacity * 0.95;
                accumColor += vec3(0.15, 2.4, 2.2) * a;
                accumAlpha += a;
            }
        }
    }

    vec4 col;
    if (accumAlpha > 0.0) {
        col.rgb = accumColor * 1.5;
        col.a = min(accumAlpha * 1.2, 1.0);
    } else {
        col = vec4(0.0, 0.0, 0.0, 0.0);
    }

    col = clamp(col, 0.0, 1.0);

    vec3 shade = vertexColor.rgb * 0.03 + vec3(1.0) * 0.97;
    col.rgb *= shade;
    col.a *= mask.r * opacity;

    fragColor = linear_fog(col * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}