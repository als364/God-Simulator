/**
 * material_particle.fp
 * 
 * Fragment shader shader which forward-shades an (unshaded, alpha-blended) particle system.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-04-15
 */
/* Material properties passed from the application. */
uniform vec4 Color;

/* Textures and flags for whether they exist. */
uniform sampler2D Texture;
uniform bool HasTexture;
uniform vec3 startPosition;
uniform vec3 maxPoint;
uniform int ID;
uniform float xlimit;


/* Some constant maximum number of lights which GLSL and Java have to agree on. */
#define MAX_LIGHTS 100


/* Uniforms describing the lights. */
uniform int NumLights;
uniform vec3 LightPositions[MAX_LIGHTS];
uniform vec3 LightAttenuations[MAX_LIGHTS];
uniform vec3 LightColors[MAX_LIGHTS];

/* Texture coordinate passed from vertex shader. */
varying vec2 TexCoord;
varying vec3 EyespacePosition;
varying vec3 EyespaceNormal;




vec3 shadeLambertian(vec3 diffuse, vec3 position, vec3 normal, vec3 lightPosition, vec3 lightColor, vec3 lightAttenuation)
{
	vec3 lightDirection = normalize(lightPosition - position);
	float ndotl = max(0.0, dot(normal, lightDirection));
		
	float r = length(lightPosition - position);
	float attenuation = 1.0 / dot(lightAttenuation, vec3(1.0, r, r * r));
	
	return lightColor * attenuation * diffuse * ndotl;
}

void main()
{

	vec3 position = EyespacePosition;
	vec3 normal = normalize(EyespaceNormal);
	
	/* Multiply color by texture, if we have one. */
	vec4 color = Color;
	float difference = maxPoint.x - startPosition.x;
	if (difference > 1.0) difference -= 1.0;
	else difference = 0.0;
		
	float maxDiff = abs(xlimit) - maxPoint.x;
	//if (abs(xlimit) != xlimit) maxDiff = x
	//if (abs(maxDiff) != maxDiff && maxPoint.x != abs(maxPoint.x)) maxDiff *= -1.0; 
	//if (abs(maxDiff) != maxDiff && maxPoint.x != maxPoint.x) maxDiff *= -1;
	if (HasTexture)
	{
		color *= texture2D(Texture, TexCoord);
		if (ID == 0 || ID == 4) color.a *= 0.4;
		if (ID == 3 || ID == 2) color.a *= 0.6;
		if (ID == 1) color.a *= 0.15;
		//color.a *= 0.8;
		if (ID == 2 || ID == 3) color.xyz *= 0.5;
	}
		if (difference < 1.0) color.a *= 1.0 - (1.0 - difference);
		//if (difference <= 0.0) color = vec4(0.0); 
		if (maxDiff < 2.0) color.a *= (maxDiff/2.0);
		if (maxDiff < 0.0) color.a *= 0.0;
		
		
		
		
		for (int i = 0; i < NumLights; i++) {
			vec3 c = color.xyz + vec3(shadeLambertian(color.xyz, position, normal, LightPositions[i], LightColors[i], LightAttenuations[i]));
			if (c.x <= 1.0 && c.y <= 1.0 && c.z <= 1.0) color.xyz = c;
		}
	
		//clip the values
		if (color.x <= 0.0 && color.y <= 0.0 && color.z <= 0.0) color.a *= 0.0;
		if (color.x > 1.0 && color.y > 1.0 && color.z > 1.0) color = vec4(1.0);
		if (color.a > 1.0) color.a = 1.0;
		
		//if( !((color.x > 0.0) && (color.y > 0.0) && (color.z > 0.0))) color *= 0.0;
	
	/* Output premultiplied color and alpha to achieve regular alpha blending.  
	 * See `ParticleMaterial.bind()` for an explanation. */ 
	gl_FragColor = vec4(color.rgb * color.a, color.a);
}
