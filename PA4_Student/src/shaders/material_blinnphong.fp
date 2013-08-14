/**
 * material_blinnphong.fp
 * 
 * Fragment shader which writes material information needed for Blinn-Phong shading to
 * the gbuffer.
 * 
 * Written for Cornell CS 5625 (Interactive Computer Graphics).
 * Copyright (c) 2012, Computer Science Department, Cornell University.
 * 
 * @author Asher Dunn (ad488)
 * @date 2012-03-24
 */

/* ID of Blinn-Phong material, so the lighting shader knows what material
 * this pixel is. */
const int BLINNPHONG_MATERIAL_ID = 3;

/* Material properties passed from the application. */
uniform vec3 DiffuseColor;
uniform vec3 SpecularColor; 
uniform float PhongExponent;

/* Textures and flags for whether they exist. */
uniform sampler2D DiffuseTexture;
uniform sampler2D SpecularTexture;

uniform bool HasDiffuseTexture;
uniform bool HasSpecularTexture;

/* Fragment position and normal, and texcoord, from vertex shader. */
varying vec3 EyespacePosition;
varying vec3 EyespaceNormal;
varying vec2 TexCoord;

void main()
{
//SOLUTION
	/* Multiply diffuse color by diffuse texture, if we have one. */
	vec3 diffuse = DiffuseColor;
	if (HasDiffuseTexture)
	{
		diffuse *= texture2D(DiffuseTexture, TexCoord).rgb;
	}
	
	/* Multiply specular color by specular texture, if we have one. The Phong exponent
	 * is multiplied by the texture's alpha channel. */
	vec4 specular = vec4(SpecularColor, PhongExponent);
	if (HasSpecularTexture)
	{
		specular *= texture2D(SpecularTexture, TexCoord);
	}
	
	/* Store {diffuse, specular, position, normal} into the gbuffer. */
	gl_FragData[0] = vec4(diffuse, float(BLINNPHONG_MATERIAL_ID));
	gl_FragData[1] = specular;
	gl_FragData[2] = vec4(EyespacePosition, 1.0);
	gl_FragData[3] = vec4(normalize(EyespaceNormal), 1.0);
//FILLIN
//	gl_FragData[0] = gl_FragData[1] = gl_FragData[2] = gl_FragData[3] = vec4(1.0);
//ENDSOLUTION
}
