package cs5625.deferred.bintree;

import javax.vecmath.Point3f;

public class Node 
{
	private Point3f[] vertices;

	private int index;
	
	private int splitBufferIndex;
	private int mergeBufferIndex;
	
	public Node(Point3f[] vertices, int index)
	{
		this.vertices = vertices;
		this.index = index;
		splitBufferIndex = -1;
		mergeBufferIndex = -1;
	}

	public Point3f[] getVertices() {
		return vertices;
	}

	public int getIndex() {
		return index;
	}

	public int getSplitBufferIndex() {
		return splitBufferIndex;
	}

	public void setSplitBufferIndex(int splitBufferIndex) {
		this.splitBufferIndex = splitBufferIndex;
	}

	public int getMergeBufferIndex() {
		return mergeBufferIndex;
	}

	public void setMergeBufferIndex(int mergeBufferIndex) {
		this.mergeBufferIndex = mergeBufferIndex;
	}
}