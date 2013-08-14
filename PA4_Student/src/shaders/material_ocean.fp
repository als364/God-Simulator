/* ID of Ocean material, so the lighting shader knows what material
 * this pixel is. */
const int OCEAN_MATERIAL_ID = 5;

/* Fragment position, normal, and texcoord passed from the vertex shader. */
varying vec3 WorldspacePosition;
varying vec3 EyespacePosition;
varying vec3 EyespaceNormal;
varying vec2 TexCoord;

/* Textures and flags for whether they exist. */
uniform vec3 DiffuseColor;
uniform sampler2D NoiseTexture;

void main()
{
	vec3 diffuse = DiffuseColor;
	vec3 noise = texture2D(NoiseTexture, TexCoord).rgb;
	
	/* Store {diffuse, [unused], position, normal} into the gbuffer. */
	gl_FragData[0] = vec4(diffuse, float(OCEAN_MATERIAL_ID));
	gl_FragData[1] = vec4(noise, WorldspacePosition.x);
	gl_FragData[2] = vec4(EyespacePosition, WorldspacePosition.y);
	gl_FragData[3] = vec4(EyespaceNormal, WorldspacePosition.z);
}