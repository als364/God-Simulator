package cs5625.deferred.bintree;


public class StaticRingBuffer
{
	private int[] indexBuffer;
	public int headIndex;
	public int tailIndex;
	
	public StaticRingBuffer(int size)
	{
		indexBuffer = new int[size];
		headIndex = 0;
		tailIndex = 0;
	}

	public void addAtFront(int node)
	{
		headIndex = headIndex--;
		if(headIndex < 0) headIndex += indexBuffer.length;
		indexBuffer[headIndex] = node;
	}
	
	public void addAtEnd(int node)
	{
		tailIndex = tailIndex++;
		if(tailIndex >= indexBuffer.length) tailIndex -= indexBuffer.length;
		indexBuffer[tailIndex] = node;
	}
	
	public int pollFromFront()
	{
		headIndex = headIndex++;
		if(headIndex >= indexBuffer.length) headIndex -= indexBuffer.length;
		if(headIndex == 0)
		{
			return indexBuffer[indexBuffer.length - 1];
		}
		else
		{
			return indexBuffer[headIndex - 1];
		}
	}
	
	public int pollFromEnd()
	{
		tailIndex = tailIndex--;
		if(tailIndex < 0) tailIndex += indexBuffer.length;
		if(tailIndex == indexBuffer.length - 1)
		{
			return indexBuffer[0];
		}
		else
		{
			return indexBuffer[tailIndex + 1];
		}
	}

	public int peekFromFront()
	{
		return indexBuffer[headIndex];
	}
	
	public int peekFromEnd()
	{
		return indexBuffer[tailIndex];
	}
}
