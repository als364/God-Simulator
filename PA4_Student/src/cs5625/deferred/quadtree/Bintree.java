package cs5625.deferred.bintree;

import javax.vecmath.Point2i;
import javax.vecmath.Point3f;

//http://members.gamedev.net/rootevilgames/mwhite/GEOmancy/Paper.pdf
public class Bintree 
{
	Node[] bintree;
	float[] variance;
	int numquads;
	int width;
	
	StaticRingBuffer splitQueue;
	StaticRingBuffer mergeQueue;

	//Invariant: Numquads is a power of 2.
	public Bintree(int numquads)	
	{
		bintree = new Node[2 * numquads];
		variance = new float[2 * numquads];
		this.numquads = numquads;
		width = (int)Math.sqrt(numquads);
		splitQueue = new StaticRingBuffer(2 * numquads);
		mergeQueue = new StaticRingBuffer(2 * numquads);
		splitQueue.addAtFront(0);
	}
	
	public void initBintree(float[][] heightmap, boolean isLeft)
	{
		//TODO: actual initialisation :|
		if(isLeft)
		{
			createNode(0, new Point2i(0, 0), new Point2i(0, width), new Point2i(width, 0), heightmap);
		}
		else
		{
			createNode(0, new Point2i(width, width), new Point2i(width, 0), new Point2i(0, width), heightmap);
		}
	}
	
	public int getParentNode(int index)
	{
		if(index == 0)
		{
			return -1;
		}
		else
		{
			return index >> 1;
		}
	}
	
	public int getLeftChild(int index)
	{
		if(index << 1 > bintree.length)
		{
			return -1;
		}
		else
		{
			return index << 1;
		}
	}
	
	public int getRightChild(int index)
	{
		if((index << 1) + 1 > bintree.length)
		{
			return -1;
		}
		else
		{
			return (index << 1) + 1;
		}
	}

	//Invariant: Between node 1 and 2 is the hypotenuse. Get it right.
	private void createNode(int nodeIndex, Point2i vert0, Point2i vert1, Point2i vert2, float[][] heightmap)
	{
		Node newNode = new Node(new Point3f[]{new Point3f(vert0.x, heightmap[vert0.x][vert0.y], vert0.y), 
								 new Point3f(vert1.x, heightmap[vert1.x][vert1.y], vert1.y), 
								 new Point3f(vert2.x, heightmap[vert2.x][vert2.y], vert2.y)}, 
								 nodeIndex);
		bintree[nodeIndex] = newNode;
		
		if(nodeIndex < numquads)
		{
			Point2i midpoint = new Point2i((vert1.x + vert2.x) / 2, (vert1.y + vert2.y) / 2);
			
			int leftChildIndex;
			int rightChildIndex;
			
			if(nodeIndex == 0)
			{
				leftChildIndex = 1;
				rightChildIndex = 2;
			}
			else
			{
				leftChildIndex = nodeIndex * 2;
				rightChildIndex = (nodeIndex * 2) + 1;
			}
			createNode(leftChildIndex, midpoint, vert2, vert0, heightmap);
			createNode(rightChildIndex, midpoint, vert0, vert1, heightmap);
			
			float heightdiff = (heightmap[vert1.x][vert1.y] + heightmap[vert2.x][vert2.y]) / 2;
			float leftChildVariance = variance[leftChildIndex];
			float rightChildVariance = variance[rightChildIndex];
			float nodeVariance = Math.max(heightdiff, Math.max(leftChildVariance, rightChildVariance));
			variance[nodeIndex] = nodeVariance;
		}
		else
		{
			variance[nodeIndex] = 0;
		}
	}

	public void split()
	{
		int triToSplit = splitQueue.peekFromFront();
		if(variance[getLeftChild(triToSplit)] > variance[getRightChild(triToSplit)])
		{
			mergeQueue.addAtFront(getLeftChild(triToSplit));
			bintree[getLeftChild(triToSplit)].setMergeBufferIndex(mergeQueue.headIndex);
			mergeQueue.addAtFront(getRightChild(triToSplit));
			bintree[getRightChild(triToSplit)].setMergeBufferIndex(mergeQueue.headIndex);
		}
		else
		{
			mergeQueue.addAtFront(getRightChild(triToSplit));
			bintree[getRightChild(triToSplit)].setMergeBufferIndex(mergeQueue.headIndex);
			mergeQueue.addAtFront(getLeftChild(triToSplit));
			bintree[getLeftChild(triToSplit)].setMergeBufferIndex(mergeQueue.headIndex);
		}
		mergeQueue.addAtFront(splitQueue.pollFromFront());
	}

	public void merge()
	{
		splitQueue.addAtFront(mergeQueue.pollFromFront());
		bintree[splitQueue.pollFromFront()].setMergeBufferIndex(-1);
		bintree[splitQueue.pollFromFront()].setSplitBufferIndex(-1);
		bintree[splitQueue.pollFromFront()].setSplitBufferIndex(-1);
	}
}
