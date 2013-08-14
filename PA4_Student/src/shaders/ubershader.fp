/**
 * ubershader.fp
 * 
 * Fragment shader for the "ubershader" which lights the contents of the gbuffer. This shader
 * samples from the gbuffer and then computes lighting depending on the material type of this 
 * fragment.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-03-24
 */

/* Copy the IDs of any new materials here. */
const int UNSHADED_MATERIAL_ID = 1;
const int LAMBERTIAN_MATERIAL_ID = 2;
const int BLINNPHONG_MATERIAL_ID = 3;
const int TERRAIN_MATERIAL_ID = 4;
const int OCEAN_MATERIAL_ID = 5;

/* Some constant maximum number of lights which GLSL and Java have to agree on. */
#define MAX_LIGHTS 100

/* Samplers for each texture of the GBuffer. */
uniform sampler2DRect DiffuseBuffer;
uniform sampler2DRect MaterialParamsBuffer;
uniform sampler2DRect PositionBuffer;
uniform sampler2DRect NormalBuffer;
uniform sampler2DRect SilhouetteBuffer;

uniform bool EnableToonShading;

/* Uniform specifying the sky (background) color. */
uniform vec3 SkyColor;

/* Uniforms describing the lights. */
uniform int NumLights;
uniform vec3 LightPositions[MAX_LIGHTS];
uniform vec3 LightAttenuations[MAX_LIGHTS];
uniform vec3 LightColors[MAX_LIGHTS];

const float DETECTION_THRESHOLD_DEPTH = 0.1;
const float DETECTION_THRESHOLD_NORM = 0.2;

/**
* Puts the magnitude of the vector consisting of the xyz components of the silhouette buffer sample in x 
* Puts the w value of the silhouette buffer sample in y
**/
vec2 sampleSilhouetteBuffer(vec2 coord)
{
	vec4 temp = texture2DRect(SilhouetteBuffer, coord);
	return vec2(sqrt(temp.x * temp.x + temp.y * temp.y + temp.z * temp.z), temp.w);
}

/**
 * Performs the "3x3 nonlinear filter" mentioned in Decaudin 1996 to detect silhouettes
 * based on the silhouette buffer.
 */
float silhouetteStrength()
{
	// TODO (compute silhouette)
	vec2 A = sampleSilhouetteBuffer(vec2(gl_FragCoord.x-1.0, gl_FragCoord.y+1.0));
	vec2 B = sampleSilhouetteBuffer(vec2(gl_FragCoord.x, gl_FragCoord.y+1.0));
	vec2 C = sampleSilhouetteBuffer(vec2(gl_FragCoord.x+1.0, gl_FragCoord.y+1.0));
	
	vec2 D = sampleSilhouetteBuffer(vec2(gl_FragCoord.x-1.0, gl_FragCoord.y));
	vec2 X = sampleSilhouetteBuffer(vec2(gl_FragCoord.xy));
	vec2 E = sampleSilhouetteBuffer(vec2(gl_FragCoord.x+1.0, gl_FragCoord.y));
	
	vec2 F = sampleSilhouetteBuffer(vec2(gl_FragCoord.x-1.0, gl_FragCoord.y-1.0));
	vec2 G = sampleSilhouetteBuffer(vec2(gl_FragCoord.x, gl_FragCoord.y-1.0));
	vec2 H = sampleSilhouetteBuffer(vec2(gl_FragCoord.x+1.0, gl_FragCoord.y-1.0));
	
	float xmax1 = max(A.x, max(B.x, C.x));
	float xmax2 = max(D.x, max(E.x, F.x));
	float xmax3 = max(G.x, max(H.x, X.x));
	float Xmax = max(xmax1, max(xmax2, xmax3));
	
	float amin = min(A.x, min(B.x, C.x));
	float bmin = min(D.x, min(E.x, F.x));
	float cmin = min(G.x, min(H.x, X.x));
	float Xmin = min(amin, min(bmin, cmin));
	
	float ymax1 = max(A.y, max(B.y, C.y));
	float ymax2 = max(D.y, max(E.y, F.y));
	float ymax3 = max(G.y, max(H.y, X.y));
	float Ymax = max(ymax1, max(ymax2, ymax3));
	
	float ymin1 = min(A.y, min(B.y, C.y));
	float ymin2 = min(D.y, min(E.y, F.y));
	float ymin3 = min(G.y, min(H.y, X.y));
	float Ymin = min(ymin1, min(ymin2, ymin3));

	float pX = min((((Xmax-Xmin)*(Xmax-Xmin))/DETECTION_THRESHOLD_NORM), 1.0);
	float pY = min((((Ymax-Ymin)*(Ymax-Ymin))/DETECTION_THRESHOLD_NORM), 1.0);

	return max(pX, pY);
}

/**
 * Performs Lambertian shading on the passed fragment data (color, normal, etc.) for a single light.
 * 
 * @param diffuse The diffuse color of the material at this fragment.
 * @param position The eyespace position of the surface at this fragment.
 * @param normal The eyespace normal of the surface at this fragment.
 * @param lightPosition The eyespace position of the light to compute lighting from.
 * @param lightColor The color of the light to apply.
 * @param lightAttenuation A vector of (constant, linear, quadratic) attenuation coefficients for this light.
 * 
 * @return The shaded fragment color; for Lambertian, this is `lightColor * diffuse * n_dot_l`.
 */
vec3 shadeLambertian(vec3 diffuse, vec3 position, vec3 normal, vec3 lightPosition, vec3 lightColor, vec3 lightAttenuation)
{
	vec3 lightDirection = normalize(lightPosition - position);
	float ndotl = max(0.0, dot(normal, lightDirection));

	// TODO (support toon shading)
	if (EnableToonShading) {
		ndotl = step(0.1, ndotl);
	}
		
	float r = length(lightPosition - position);
	float attenuation = 1.0 / dot(lightAttenuation, vec3(1.0, r, r * r));
		
	return lightColor * attenuation * diffuse * ndotl;
}

/**
 * Performs Blinn-Phong shading on the passed fragment data (color, normal, etc.) for a single light.
 *  
 * @param diffuse The diffuse color of the material at this fragment.
 * @param specular The specular color of the material at this fragment, with the Phong 
 *        exponent packed into the alpha channel. 
 * @param position The eyespace position of the surface at this fragment.
 * @param normal The eyespace normal of the surface at this fragment.
 * @param lightPosition The eyespace position of the light to compute lighting from.
 * @param lightColor The color of the light to apply.
 * @param lightAttenuation A vector of (constant, linear, quadratic) attenuation coefficients for this light.
 * 
 * @return The shaded fragment color.
 */
vec3 shadeBlinnPhong(vec3 diffuse, vec4 specular, vec3 position, vec3 normal, vec3 lightPosition, vec3 lightColor, vec3 lightAttenuation)
{
	vec3 viewDirection = -normalize(position);
	vec3 lightDirection = normalize(lightPosition - position);
	vec3 halfDirection = normalize(lightDirection + viewDirection);
		
	float ndotl = max(0.0, dot(normal, lightDirection));
	float ndoth = max(0.0, dot(normal, halfDirection)) * step(0.0, ndotl);
	
	// TODO (support toon shading)
	if (EnableToonShading) {
		ndotl = step(0.1, ndotl);
		ndoth = step(0.9, ndoth);
	}
	
	float r = length(lightPosition - position);
	float attenuation = 1.0 / dot(lightAttenuation, vec3(1.0, r, r * r));
	
	return lightColor * attenuation * (diffuse * ndotl + specular.rgb * pow(ndoth, specular.a));
}

vec3 shadeTerrain(vec3 diffuse, float height, vec3 position, vec3 normal, vec3 lightPosition, vec3 lightColor, vec3 lightAttenuation)
{
	vec3 viewDirection = -normalize(position);
	vec3 lightDirection = normalize(lightPosition - position);
	vec3 halfDirection = normalize(lightDirection + viewDirection);
	float ndotl = max(0.0, dot(normal, lightDirection));
	float ndoth = max(0.0, dot(normal, halfDirection)) * step(0.0, ndotl);

	// TODO (support toon shading)
	if (EnableToonShading) {
		ndotl = step(0.1, ndotl);
		ndoth = step(0.9, ndoth);
	}
		
	float r = length(lightPosition - position);
	float attenuation = 1.0 / dot(lightAttenuation, vec3(1.0, r, r * r));
	
	if(height < 25.0)
	{
		return lightColor * attenuation * diffuse * ndotl;
	}
	else
	{
		return lightColor * attenuation * (diffuse * ndotl + diffuse.rgb * pow(ndoth, 10.0));
	}
	//return diffuse;
}

vec3 shadeOcean(vec3 diffuse, vec3 noise, vec3 worldspacePos, vec3 position, vec3 normal, vec3 lightPosition, vec3 lightColor, vec3 lightAttenuation)
{
	vec3 viewDirection = -normalize(position);
	vec3 lightDirection = normalize(lightPosition - position);
	vec3 halfDirection = normalize(lightDirection + viewDirection);
	
	float ndotl = max(0.0, dot(normal, lightDirection));
	float ndoth = max(0.0, dot(normal, halfDirection)) * step(0.0, ndotl);
	float e = 0.001;
	
	// TODO (support toon shading)
	if (EnableToonShading) {
		ndotl = step(0.1, ndotl);
		ndoth = step(0.9, ndoth);
	}
	
	float r = length(lightPosition - position);
	float attenuation = 1.0 / dot(lightAttenuation, vec3(1.0, r, r * r));
	
	return lightColor * attenuation * diffuse * ndotl;
}

void main()
{
	/* Sample gbuffer. */
	vec4 diffuse        = texture2DRect(DiffuseBuffer, gl_FragCoord.xy);
	vec4 materialParams = texture2DRect(MaterialParamsBuffer, gl_FragCoord.xy);
	vec3 position       = texture2DRect(PositionBuffer, gl_FragCoord.xy).xyz;
	vec3 normal         = normalize(texture2DRect(NormalBuffer, gl_FragCoord.xy).xyz);
	vec3 oceanwspos     = vec3(texture2DRect(MaterialParamsBuffer, gl_FragCoord.xy).a, 
							   texture2DRect(PositionBuffer, gl_FragCoord.xy).a,
							   texture2DRect(NormalBuffer, gl_FragCoord.xy).a);
	
	vec4 X = texture2DRect(SilhouetteBuffer, vec2(gl_FragCoord.xy));
			
	/* Initialize fragment to black. */
	gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);

	/* Branch on material ID and shade as appropriate. */
	int materialID = int(diffuse.a);

	if (materialID == 0)
	{
		/* Must be a fragment with no geometry, so set to sky (background) color. */
		gl_FragColor = vec4(SkyColor, 1.0);
	}
	else if (materialID == UNSHADED_MATERIAL_ID)
	{
		/* Unshaded material is just a constant color. */
		gl_FragColor.rgb = diffuse.rgb;
	}
	
	// TODO (support additional materials)
	else if (materialID == LAMBERTIAN_MATERIAL_ID)
	{	
		vec3 color = vec3(0.0);

		for (int i = 0; i < NumLights; i++) {
			color += shadeLambertian(diffuse.xyz, position, normal, LightPositions[i], LightColors[i], LightAttenuations[i]);
		}
		gl_FragColor.rgb = color.rgb;
	}
	
	else if (materialID == BLINNPHONG_MATERIAL_ID)
	{
		vec3 color = vec3(0.0);
		
		for (int i = 0; i < NumLights; i++) {
			color += shadeBlinnPhong(diffuse.xyz, materialParams, position, normal, LightPositions[i], LightColors[i], LightAttenuations[i]);
		}
		gl_FragColor.rgb = color.rgb;
	}
	
	else if (materialID == TERRAIN_MATERIAL_ID)
	{
		vec3 color = vec3(0.0);
		
		for (int i = 0; i < NumLights; i++) {
			color += shadeTerrain(diffuse.xyz, materialParams.x, position, normal, LightPositions[i], LightColors[i], LightAttenuations[i]);
		}
		
		gl_FragColor.rgb = color.rgb;
	}
	
	else if (materialID == OCEAN_MATERIAL_ID)
	{
		vec3 color = vec3(0.0);
		
		for (int i = 0; i < NumLights; i++) {
			color += shadeOcean(diffuse.xyz, materialParams.xyz, oceanwspos, position, normal, LightPositions[i], LightColors[i], LightAttenuations[i]);
		}
		
		gl_FragColor.rgb = color.rgb;
	}
	
	else
	{
		/* Unknown material, so just use the diffuse color. */
		gl_FragColor = vec4(diffuse.rgb, 1.0);
	}

	// TODO (support toon shading)
	if (EnableToonShading) {
		float p = silhouetteStrength();
		gl_FragColor = gl_FragColor * (1.0 - p);
		
	}
}
