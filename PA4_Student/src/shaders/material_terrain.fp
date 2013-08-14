/* ID of Terrain material, so the lighting shader knows what material
 * this pixel is. */
const int TERRAIN_MATERIAL_ID = 4;

/* Fragment position, normal, and texcoord passed from the vertex shader. */
varying vec3 WorldspacePosition;
varying vec3 EyespacePosition;
varying vec3 EyespaceNormal;
varying vec2 TexCoord;

/* Textures and flags for whether they exist. */
uniform sampler2D GrassTexture;
uniform sampler2D RockTexture;
uniform sampler2D SnowTexture;
uniform sampler2D SandTexture;
uniform int texWidth;

float interpolate(float x0, float x1, float alpha)
{
   return x0 * (1.0 - alpha) + alpha * x1;
}

void main()
{
	/* Multiply diffuse color by diffuse texture, if we have one. */
	vec3 diffuse = vec3(0);
	if(WorldspacePosition.y < 5.5)
	{
		diffuse = texture2D(SandTexture, TexCoord).rgb;
	}
	else if (WorldspacePosition.y >= 5.5 && WorldspacePosition.y < 7.0)
	{
		vec3 grass = texture2D(GrassTexture, TexCoord).rgb;
		vec3 sand = texture2D(SandTexture, TexCoord).rgb;
		float t = (WorldspacePosition.y - 5.5) / 1.5;
		diffuse = vec3(interpolate(sand.r, grass.r, t), interpolate(sand.g, grass.g, t), interpolate(sand.b, grass.b, t));
	}
	else if (WorldspacePosition.y >= 7.0 && WorldspacePosition.y < 10.0)
	{
		diffuse = texture2D(GrassTexture, TexCoord).rgb;
	}
	else if(WorldspacePosition.y >= 10.0 && WorldspacePosition.y < 12.0)
	{
		vec3 grass = texture2D(GrassTexture, TexCoord).rgb;
		vec3 rock = texture2D(RockTexture, TexCoord).rgb;
		float t = (WorldspacePosition.y - 10.0) / 2.0;
		diffuse = vec3(interpolate(grass.r, rock.r, t), interpolate(grass.g, rock.g, t), interpolate(grass.b, rock.b, t));
	}
	else if(WorldspacePosition.y >= 12.0 && WorldspacePosition.y < 20.0)
	{
		diffuse = texture2D(RockTexture, TexCoord).rgb;
	}
	else if(WorldspacePosition.y >= 20.0 && WorldspacePosition.y < 25.0)
	{
		vec3 snow = texture2D(SnowTexture, TexCoord).rgb;
		vec3 rock = texture2D(RockTexture, TexCoord).rgb;
		float t = (WorldspacePosition.y - 20.0) / 5.0;
		diffuse = vec3(interpolate(rock.r, snow.r, t), interpolate(rock.g, snow.g, t), interpolate(rock.b, snow.b, t));
	}
	else
	{
		diffuse = texture2D(SnowTexture, TexCoord).rgb;
	}
	
	/* Store {diffuse, [unused], position, normal} into the gbuffer. */
	gl_FragData[0] = vec4(diffuse, float(TERRAIN_MATERIAL_ID));
	gl_FragData[1] = vec4(WorldspacePosition.y, 0.0, 0.0, 0.0);
	gl_FragData[2] = vec4(EyespacePosition, 1.0);
	gl_FragData[3] = vec4(EyespaceNormal, 1.0);
}
