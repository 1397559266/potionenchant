#version 150

#define MAX_STEPS 300
#define MAX_DIST 200.0
#define SURF_DIST 0.01

uniform float time;
uniform float iZoom;
uniform vec2 screenSize;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

uniform float yaw;
uniform float pitch;

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;
in vec3 worldPos;

out vec4 fragColor;

// ============================================
// 黑洞参数配置（Interstellar 风格）
// ============================================
// 如何调整黑洞大小：
// 1. 整体缩放：同时修改 DISK_INNER、DISK_OUTER、DISK_THICKNESS
//    - 想要更大：所有值乘以相同倍数（如 2.0）
//    - 想要更小：所有值除以相同倍数（如 0.5）
// 2. 事件视界（黑色中心）：只修改 BH_RADIUS
//    - 增大：黑色部分变大
//    - 减小：黑色部分变小，甚至完全移除（设为 0）
// 3. 吸积盘比例：调整 DISK_INNER 和 DISK_OUTER 的差值
//    - 差值越大：光环越宽
//    - 差值越小：光环越窄
// ============================================

const float BH_RADIUS = 0.12;         // 事件视界半径（黑色中心的大小）
// 建议范围：0.0（无黑心）~ 2.0（大黑洞）
// 当前值很小，几乎看不见黑色部分

const float DISK_INNER = 1.1;         // 吸积盘内径（光环开始的位置）
// 必须大于 BH_RADIUS
// 增大：光环离中心更远
// 减小：光环更靠近中心

const float DISK_OUTER = 6.0;          // 吸积盘外径（光环结束的位置）
// 必须大于 DISK_INNER
// 增大：光环更宽，延伸更远
// 减小：光环更紧凑

const float DISK_THICKNESS = 0.17;     // 吸积盘厚度（垂直方向的高度）
// 增大：光环更厚，像甜甜圈
// 减小：光环更薄，像纸片

const float PHOTON_RING = 1.4;         // 光子环半径（预留参数，暂未使用）
// 理论值约为 BH_RADIUS * 1.4

// 引力透镜 - 使光线在黑洞周围弯曲
vec3 applyGravitationalLensing(vec3 rayDir, vec3 pos) {
    float dist = length(pos);
    if (dist < BH_RADIUS * 0.5) return rayDir;
    
    // 简化的史瓦西度规近似
    vec3 toCenter = normalize(-pos);
    float bendingStrength = (BH_RADIUS * 2.5) / (dist * dist);
    
    // 将光线弯向黑洞
    vec3 bentDir = normalize(rayDir + toCenter * bendingStrength);
    return bentDir;
}

// 获取吸积盘颜色，包含多普勒聚束和相对论红移效果
vec3 getAccretionDisk(vec3 pos, vec3 viewDir) {
    float distToCenter = length(pos);
    
    // 检查是否在吸积盘平面内（考虑厚度）
    float height = abs(pos.y);
    float maxThickness = DISK_THICKNESS * (1.0 + (distToCenter - DISK_INNER) / (DISK_OUTER - DISK_INNER));
    
    if (height > maxThickness) {
        return vec3(0.0);
    }
    
    // 平滑径向衰减 - 防止边缘硬截断
    // 内边缘渐隐
    float innerFade = smoothstep(DISK_INNER * 0.8, DISK_INNER, distToCenter);
    
    // 外边缘渐隐 - 逐渐 taper 而非硬截断
    // 更早开始渐隐（在外径的 30% 处）以获得更宽、更自然的过渡
    // 这创造了柔和的尖端边缘，而非方形截断
    float outerFade = 1.0 - smoothstep(DISK_OUTER * 0.3, DISK_OUTER, distToCenter);
    
    // 组合径向因子
    float radialFactor = innerFade * outerFade;
    
    if (radialFactor <= 0.0 || distToCenter < DISK_INNER * 0.5 || distToCenter > DISK_OUTER * 1.3) {
        return vec3(0.0);
    }
    
    // 温度梯度 - 内部更热（白色），外部更冷（红色）
    float tempRatio = (distToCenter - DISK_INNER) / (DISK_OUTER - DISK_INNER);
    vec3 hotColor = vec3(1.0, 0.95, 0.85);   // 白热中心
    vec3 coolColor = vec3(1.0, 0.5, 0.15);   // 橙红边缘
    vec3 baseColor = mix(hotColor, coolColor, tempRatio);
    
    // 多普勒聚束 - 接近侧更亮，远离侧更暗
    vec3 diskVelocity = normalize(cross(vec3(0.0, 1.0, 0.0), pos));
    float doppler = dot(diskVelocity, normalize(viewDir));
    float beamFactor = 1.0 + doppler * 0.7; // 强不对称性，类似《星际穿越》
    
    // 相对论红移/蓝移
    float shiftFactor = 1.0 + doppler * 0.3;
    if (shiftFactor < 0.5) shiftFactor = 0.5;
    baseColor *= shiftFactor;
    
    // 强度随距离衰减
    float intensity = exp(-(distToCenter - DISK_INNER) * 0.15);
    
    // 垂直衰减 - 更尖锐的边缘（指数越高越尖锐）
    // 使用 1.5 次方让边缘像刀刃一样更尖锐地 taper
    float verticalFade = pow(1.0 - height / maxThickness, 1.5);
    
    // 旋转动画
    float angle = atan(pos.z, pos.x) + time * 0.3;
    float rotationPattern = sin(angle * 4.0 + pos.y * 10.0) * 0.1 + 0.9;
    
    // 组合所有因子，包含平滑径向衰减
    intensity *= beamFactor * verticalFade * rotationPattern * radialFactor;
    
    return baseColor * intensity * 4.0;
}

void main() {
    // 采样遮罩纹理
    vec4 mask = texture(Sampler0, texCoord0);
    if (mask.r < 0.01) {
        discard;
    }
    
    vec4 clipPos = ProjMat * ModelViewMat * vec4(worldPos, 1.0);
    vec2 ndc = clipPos.xy / clipPos.w;
    vec2 uv = ndc * 0.5;
    
    float aspectRatio = screenSize.x / screenSize.y;
    vec2 displayUV = uv;
    displayUV.x *= aspectRatio;
    
    // 创建射线方向 - 使用iZoom控制缩放
    float effectiveZoom = iZoom > 0.0 ? iZoom : 1.0;
    vec3 dir = vec3(displayUV * 0.8 * effectiveZoom, 1.0);
    vec3 rayDir = normalize(dir);
    
    // 应用相机旋转
    float cp = cos(-pitch);
    float sp = sin(-pitch);
    rayDir = vec3(rayDir.x, rayDir.y * cp - rayDir.z * sp, rayDir.y * sp + rayDir.z * cp);
    
    float cy = cos(yaw);
    float sy = sin(yaw);
    rayDir = vec3(rayDir.x * cy + rayDir.z * sy, rayDir.y, -rayDir.x * sy + rayDir.z * cy);
    
    // 带引力透镜的光线步进
    vec3 col = vec3(0.0); // 从纯黑背景开始
    vec3 from = vec3(0.0, 0.0, -25.0);
    vec3 currentPos = from;
    vec3 currentDir = rayDir;
    float t = 0.0;
    
    for (int i = 0; i < MAX_STEPS; i++) {
        float distToCenter = length(currentPos);
        
        // 无事件视界阻挡 - 让光线穿过中心
        // 这创造了更超现实、艺术化的效果
        
        // 对射线方向应用引力透镜
        currentDir = applyGravitationalLensing(currentDir, currentPos);
        
        // 采样吸积盘
        vec3 diskCol = getAccretionDisk(currentPos, currentDir);
        col += diskCol;
        
        // 无背景星星 - 纯黑空间
        
        // 自适应步长 - 靠近黑洞时更小以提高质量
        float stepSize = max(distToCenter * 0.08, 0.3);
        currentPos += currentDir * stepSize;
        t += stepSize;
        
        if (t > MAX_DIST) break;
    }
    
    // 应用遮罩
    col *= mask.r;
    float alpha = min(length(col), 1.0);
    
    fragColor = vec4(col, alpha);
}
