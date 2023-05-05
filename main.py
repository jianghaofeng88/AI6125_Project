#导入必须的包
import matplotlib.pyplot as plt
import numpy as np
ys=[0,0,0,0,0,0]
colors=['r','g','b','c','b','y']
#plt.figure(figsize=(8, 8))
for i in [2]:
    

    #-----------  打开txt文件   ----------
    file = open('data100voyage'+str(i)+'.txt')
    #-----------  逐行读取文件内的数据  ------------
    data = file.readlines()
    #-----------  根据自己的需要查看data的内容  ---------
    #print(data)
    '''
    txt文件的数值为y轴的数据
    所以x要根据y的个数有序生成
    '''
    y=[]
    x=np.arange(10)
    for num in data:
        y.append(float(num.split(',')[0]))
	
    ys[i] = y
    file.close()


for i in [2]:
    plt.plot(x,ys[i],label='Each score',color=colors[i],linewidth=1)
    plt.axhline(sum(ys[i])/len(ys[i]), linestyle="--", label='Average score', color=colors[i])


#---------   x轴的小标题   -------------
plt.xlabel('Epoch')
#---------   y轴的小标题   -------------
plt.ylabel('score')
#---------   整个图的标题  ----------
plt.title('Presentation Result (100x100)')
plt.legend()
plt.show()
