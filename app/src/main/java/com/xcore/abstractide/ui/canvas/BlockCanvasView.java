package com.xcore.abstractide.ui.canvas;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.*;
import android.view.animation.DecelerateInterpolator;

import com.xcore.abstractide.core.model.BlockModel;
import com.xcore.abstractide.core.model.Connection;

import java.util.*;

public class BlockCanvasView extends View {

    private static final float GRID_SIZE = 20f;
    private static final float BLOCK_RADIUS = 10f;
    private static final float PORT_RADIUS = 6f;
    private static final float HEADER_HEIGHT = 24f;
    private static final float NEST_HOVER_DELAY = 800f;
    private static final float CHILD_INDENT = 25f;
    private static final float ZOOM_MIN = 0.1f;
    private static final float ZOOM_MAX = 5.0f;
    private static final long ANIM_DURATION = 300;
    private static final long DOUBLE_CLICK_INTERVAL = 300;
    private static final int PORT_INPUT_COLOR = 0xFFe74c3c;
    private static final int PORT_OUTPUT_COLOR = 0xFF2ecc71;
    private static final int CONNECTION_COLOR = 0xFF3498db;
    private static final int NEST_READY_COLOR = 0xFF2ecc71;
    private static final int SWAP_COLOR = 0xFFf39c12;
    private static final int SELECTION_COLOR = 0xFFf39c12;
    private static final int CELL_COLOR = 0xFF3a3a3a;
    private static final int CELL_BORDER_COLOR = 0xFF555555;

    private static final Set<String> CONTAINER_TYPES = new HashSet<>(Arrays.asList(
            "DataTypes.List", "DataTypes.Dict", "DataTypes.Tuple", "DataTypes.Set",
            "Functions.Function", "Functions.Method",
            "ControlFlow.If", "ControlFlow.For", "ControlFlow.While",
            "ControlFlow.Switch", "ControlFlow.Case",
            "DictOperations.Dict", "Definitions.Function", "Definitions.Class"
    ));

    private final Map<Integer, DrawableBlock> blocks = new LinkedHashMap<>();
    private final Map<String, DrawableConnection> connections = new LinkedHashMap<>();
    private final List<AnimTask> animQueue = new ArrayList<>();

    private float scaleFactor = 1f;
    private float panX, panY;
    private float lastTouchX, lastTouchY;
    private boolean isPanning;
    private float initialSpan;

    private DrawableBlock draggingBlock;
    private float dragOffsetX, dragOffsetY;
    private boolean isDraggingBlock;

    private boolean isDraggingConnection;
    private DrawableBlock connectionSource;
    private String dragStartPort;
    private PointF tempLineStart, tempLineEnd;

    private DrawableBlock nestTarget;
    private DrawableBlock swapTarget;
    private long nestHoverStart;
    private boolean nestingReady;

    private long lastClickTime;
    private DrawableBlock contextMenuBlock;
    private float contextMenuX, contextMenuY;
    private boolean showContextMenu;
    private final RectF contextMenuRect = new RectF();

    private final Set<Integer> collapsedBlocks = new HashSet<>();

    private final Paint blockPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint smallTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint();
    private final Paint portPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint connectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tempLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint nestPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint swapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cellPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path connectionPath = new Path();
    private final DecelerateInterpolator interpolator = new DecelerateInterpolator();

    private float worldMinX = -5000, worldMinY = -5000;
    private float worldMaxX = 5000, worldMaxY = 5000;

    private OnBlockClickListener blockClickListener;
    private OnCanvasChangeListener canvasChangeListener;

    public BlockCanvasView(Context context) { super(context); init(); }
    public BlockCanvasView(Context context, AttributeSet attrs) { super(context, attrs); init(); }

    private void init() {
        float d = getResources().getDisplayMetrics().density;
        textPaint.setColor(Color.WHITE); textPaint.setTextSize(12f * d); textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        smallTextPaint.setColor(0xFF888888); smallTextPaint.setTextSize(9f * d);
        gridPaint.setColor(0x33333333); gridPaint.setStrokeWidth(1f);
        tempLinePaint.setColor(Color.WHITE); tempLinePaint.setStrokeWidth(2f); tempLinePaint.setPathEffect(new DashPathEffect(new float[]{8, 4}, 0)); tempLinePaint.setStyle(Paint.Style.STROKE);
        connectionPaint.setColor(CONNECTION_COLOR); connectionPaint.setStrokeWidth(2f); connectionPaint.setStyle(Paint.Style.STROKE); connectionPaint.setStrokeCap(Paint.Cap.ROUND);
        nestPaint.setStyle(Paint.Style.STROKE); nestPaint.setStrokeWidth(3f);
        swapPaint.setStyle(Paint.Style.STROKE); swapPaint.setStrokeWidth(3f); swapPaint.setColor(SWAP_COLOR);
        selectionPaint.setColor(SELECTION_COLOR); selectionPaint.setStrokeWidth(3f); selectionPaint.setStyle(Paint.Style.STROKE);
        cellPaint.setColor(CELL_COLOR); cellPaint.setStrokeWidth(1f); cellPaint.setStyle(Paint.Style.STROKE);
        portPaint.setStyle(Paint.Style.FILL);
        setFocusable(true); setFocusableInTouchMode(true);
    }

    public static boolean isContainerType(BlockModel.BlockType type) { return type != null && CONTAINER_TYPES.contains(type.getFullName()); }

    private static class AnimTask { DrawableBlock b; float fx, fy, tx, ty; long st; AnimTask(DrawableBlock b, float fx, float fy, float tx, float ty) { this.b=b; this.fx=fx; this.fy=fy; this.tx=tx; this.ty=ty; st=System.currentTimeMillis(); } }
    private void animateTo(DrawableBlock db, float toX, float toY) { toX=clampX(toX); toY=clampY(toY); animQueue.add(new AnimTask(db, db.x, db.y, toX, toY)); if(animQueue.size()==1) startAnimLoop(); }
    private void startAnimLoop() { postOnAnimation(new Runnable(){ @Override public void run() { if(animQueue.isEmpty()) return; Iterator<AnimTask> it=animQueue.iterator(); while(it.hasNext()){ AnimTask t=it.next(); long e=System.currentTimeMillis()-t.st; float f=Math.min(1f,(float)e/ANIM_DURATION); f=interpolator.getInterpolation(f); t.b.x=clampX(t.fx+(t.tx-t.fx)*f); t.b.y=clampY(t.fy+(t.ty-t.fy)*f); t.b.model.getPosition().put("x",(double)t.b.x); t.b.model.getPosition().put("y",(double)t.b.y); if(f>=1f) it.remove(); } invalidate(); if(!animQueue.isEmpty()) postOnAnimation(this); }}); }

    private void showContextMenuForBlock(DrawableBlock db, float sx, float sy) { contextMenuBlock=db; contextMenuX=sx; contextMenuY=sy; showContextMenu=true; invalidate(); }
    private void hideContextMenu() { showContextMenu=false; contextMenuBlock=null; invalidate(); }
    private String getContextMenuAction(float x, float y) { if(!showContextMenu||contextMenuBlock==null) return null; float iy=contextMenuY+40; if(x>=contextMenuX&&x<=contextMenuX+200&&y>=iy&&y<=iy+40) return "exit"; return null; }

    private float clampX(float x) { return Math.max(worldMinX, Math.min(worldMaxX, x)); }
    private float clampY(float y) { return Math.max(worldMinY, Math.min(worldMaxY, y)); }

    public void addBlock(BlockModel block) { DrawableBlock db=new DrawableBlock(); db.model=block; db.id=block.getId(); db.x=clampX(block.getPosition().getOrDefault("x",0.0).floatValue()); db.y=clampY(block.getPosition().getOrDefault("y",0.0).floatValue()); blocks.put(block.getId(),db); invalidate(); }
    public void removeBlock(int blockId) { DrawableBlock r=blocks.remove(blockId); if(r!=null){ for(int c:r.model.getChildrenIds()) removeBlock(c); } connections.entrySet().removeIf(e->e.getValue().fromBlockId==blockId||e.getValue().toBlockId==blockId); invalidate(); }
    public void clearBlocks() { blocks.clear(); connections.clear(); animQueue.clear(); invalidate(); }
    public void addConnection(Connection conn) { DrawableConnection dc=new DrawableConnection(); dc.id=conn.getId(); dc.fromBlockId=conn.getFromBlockId(); dc.toBlockId=conn.getToBlockId(); dc.fromPort=conn.getFromPort(); dc.toPort=conn.getToPort(); connections.put(conn.getId(),dc); invalidate(); }
    public void removeConnection(String id) { connections.remove(id); invalidate(); }
    public void selectBlock(int blockId) { for(DrawableBlock db:blocks.values()) db.selected=(db.id==blockId); invalidate(); }
    public DrawableBlock getDrawableBlock(int blockId) { return blocks.get(blockId); }
    public float[] getViewCenter() { return new float[]{(getWidth()/2f-panX)/scaleFactor,(getHeight()/2f-panY)/scaleFactor}; }
    public void centerOnBlock(int blockId) { DrawableBlock db=blocks.get(blockId); if(db==null) return; panX=getWidth()/2f-(db.x+getBlockWidth(db)/2)*scaleFactor; panY=getHeight()/2f-(db.y+getBlockHeight(db)/2)*scaleFactor; clampPan(); invalidate(); }
    private void clampPan() { panX=Math.max(-worldMaxX*scaleFactor+getWidth(),Math.min(-worldMinX*scaleFactor,panX)); panY=Math.max(-worldMaxY*scaleFactor+getHeight(),Math.min(-worldMinY*scaleFactor,panY)); }

    public void onConnectionToContainer(int fromBlockId, int toBlockId) { DrawableBlock from=blocks.get(fromBlockId),to=blocks.get(toBlockId); if(from!=null&&to!=null&&to.model.isContainerBlock()) createCallBlock(from,to); }
    public void createCallBlock(DrawableBlock source, DrawableBlock container) { if(!container.model.isContainerBlock()) return; String cn="call_"+(source.model.getName()!=null?source.model.getName():"block"); int cid=container.id*1000+source.id; if(blocks.containsKey(cid)) return; BlockModel cm=new BlockModel(); cm.setId(cid); cm.setType(new BlockModel.BlockType("code","Calls","Call")); cm.setName(cn); cm.setColor(source.model.getColor()); cm.getPosition().put("x",0.0); cm.getPosition().put("y",0.0); cm.getSize().put("width",90.0); cm.getSize().put("height",24.0); cm.setParentId(container.id); cm.getProperties().put("_is_call_block",true); cm.initTransients(); addBlock(cm); container.model.getChildrenIds().add(cid); container.model.addChild(cid); invalidate(); }

    public void nestBlock(int childId, int parentId) { DrawableBlock child=blocks.get(childId),parent=blocks.get(parentId); if(child==null||parent==null||child==parent) return; if(wouldCreateCycle(child,parent)) return; if(child.model.getParentId()!=null){ DrawableBlock old=blocks.get(child.model.getParentId()); if(old!=null) old.model.getChildrenIds().remove((Integer)childId); } parent.model.getChildrenIds().add(childId); parent.model.addChild(childId); child.model.setParentId(parentId); invalidate(); if(canvasChangeListener!=null) canvasChangeListener.onBlockNested(childId,parentId); }
    private boolean wouldCreateCycle(DrawableBlock child, DrawableBlock parent) { DrawableBlock cur=parent; while(cur!=null){ if(cur==child) return true; if(cur.model.getParentId()==null) break; cur=blocks.get(cur.model.getParentId()); } return false; }
    private void swapChildren(DrawableBlock a, DrawableBlock b) { Integer pid=a.model.getParentId(); if(pid==null||!pid.equals(b.model.getParentId())) return; DrawableBlock p=blocks.get(pid); if(p==null) return; List<Integer> ch=p.model.getChildrenIds(); int ia=ch.indexOf(a.id),ib=ch.indexOf(b.id); if(ia>=0&&ib>=0){ ch.set(ia,b.id); ch.set(ib,a.id); float ax=a.x,ay=a.y; animateTo(a,b.x,b.y); animateTo(b,ax,ay); } }
    public void toggleCollapse(int blockId) { if(collapsedBlocks.contains(blockId)) collapsedBlocks.remove(blockId); else collapsedBlocks.add(blockId); invalidate(); }
    public void setOnBlockClickListener(OnBlockClickListener l) { this.blockClickListener=l; }
    public void setOnCanvasChangeListener(OnCanvasChangeListener l) { this.canvasChangeListener=l; }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); canvas.save(); canvas.translate(panX,panY); canvas.scale(scaleFactor,scaleFactor);
        drawGrid(canvas);
        blockPaint.setColor(0x30ff0000); blockPaint.setStyle(Paint.Style.STROKE); blockPaint.setStrokeWidth(4f); canvas.drawRect(worldMinX,worldMinY,worldMaxX,worldMaxY,blockPaint);
        for(DrawableConnection dc:connections.values()){ DrawableBlock f=blocks.get(dc.fromBlockId),t=blocks.get(dc.toBlockId); if(f!=null&&t!=null&&!collapsedBlocks.contains(f.id)&&!collapsedBlocks.contains(t.id)) drawConnection(canvas,f,t); }
        if(isDraggingConnection&&tempLineStart!=null&&tempLineEnd!=null) canvas.drawLine(tempLineStart.x,tempLineStart.y,tempLineEnd.x,tempLineEnd.y,tempLinePaint);
        for(DrawableBlock db:blocks.values()){ if(db.model.getParentId()==null||!collapsedBlocks.contains(db.model.getParentId())) drawBlock(canvas,db); }
        if(showContextMenu&&contextMenuBlock!=null){ float mx=contextMenuX,my=contextMenuY,mw=200,mh=70; contextMenuRect.set(mx,my,mx+mw,my+mh); blockPaint.setColor(0xEE2d2d2d); blockPaint.setStyle(Paint.Style.FILL); canvas.drawRoundRect(contextMenuRect,12,12,blockPaint); smallTextPaint.setColor(0xFF888888); canvas.drawText(contextMenuBlock.model.getName(),mx+16,my+24,smallTextPaint); RectF eb=new RectF(mx+8,my+34,mx+192,my+62); blockPaint.setColor(0xFFe74c3c); canvas.drawRoundRect(eb,8,8,blockPaint); textPaint.setColor(Color.WHITE); textPaint.setTextSize(11f*getResources().getDisplayMetrics().density); canvas.drawText("Exit",mx+76,my+56,textPaint); textPaint.setTextSize(12f*getResources().getDisplayMetrics().density); }
        canvas.restore();
    }

    private void drawGrid(Canvas canvas) { float l=-panX/scaleFactor-100,t=-panY/scaleFactor-100,r=l+getWidth()/scaleFactor+200,b=t+getHeight()/scaleFactor+200; for(float x=(float)(Math.floor(l/GRID_SIZE)*GRID_SIZE);x<=r;x+=GRID_SIZE)canvas.drawLine(x,t,x,b,gridPaint); for(float y=(float)(Math.floor(t/GRID_SIZE)*GRID_SIZE);y<=b;y+=GRID_SIZE)canvas.drawLine(l,y,r,y,gridPaint); }

    private void drawBlock(Canvas canvas, DrawableBlock db) {
        float w=getBlockWidth(db),h=getBlockHeight(db); RectF rect=new RectF(db.x,db.y,db.x+w,db.y+h); int color=parseColor(db.model.getColor());
        blockPaint.setColor(0x40000000); blockPaint.setStyle(Paint.Style.FILL); canvas.drawRoundRect(new RectF(rect.left+2,rect.top+2,rect.right+2,rect.bottom+2),BLOCK_RADIUS,BLOCK_RADIUS,blockPaint);
        blockPaint.setColor(darken(color)); canvas.drawRoundRect(rect,BLOCK_RADIUS,BLOCK_RADIUS,blockPaint);
        RectF hdr=new RectF(db.x,db.y,db.x+w,db.y+HEADER_HEIGHT); blockPaint.setColor(color); canvas.drawRoundRect(hdr,BLOCK_RADIUS,BLOCK_RADIUS,blockPaint);
        canvas.drawRect(new RectF(db.x+BLOCK_RADIUS,db.y+HEADER_HEIGHT-BLOCK_RADIUS,db.x+w-BLOCK_RADIUS,db.y+HEADER_HEIGHT),blockPaint);
        String prefix=db.model.isCallBlock()?"⚡":"";
        String name=db.model.getName(); if(name!=null)canvas.drawText(prefix+name,db.x+10,db.y+HEADER_HEIGHT-6,textPaint);
        String type=db.model.getType()!=null?db.model.getType().getSubclassName():""; if(!type.isEmpty())canvas.drawText(type,db.x+10,db.y+HEADER_HEIGHT+16,smallTextPaint);
        portPaint.setColor(PORT_INPUT_COLOR); canvas.drawCircle(db.x,db.y+h/2,PORT_RADIUS,portPaint);
        portPaint.setColor(PORT_OUTPUT_COLOR); canvas.drawCircle(db.x+w,db.y+h/2,PORT_RADIUS,portPaint);

        if(db.model.isContainerBlock()){
            blockPaint.setColor(Color.WHITE); blockPaint.setStyle(Paint.Style.STROKE); blockPaint.setStrokeWidth(2f);
            blockPaint.setPathEffect(new DashPathEffect(new float[]{8,4},0)); canvas.drawRoundRect(rect,BLOCK_RADIUS,BLOCK_RADIUS,blockPaint); blockPaint.setPathEffect(null);
            String ct=db.model.getContainerType(); int cc=db.model.getChildrenIds().size();

            if("dictionary".equals(ct)){
                float cellH=24,cellPadding=8; float startX=db.x+w+10; float startY=db.y+8; int pairCount=Math.max(1,(cc+1)/2);
                float maxKeyW=60,maxValW=60;
                for(int i=0;i<cc;i++){ DrawableBlock child=blocks.get(db.model.getChildrenIds().get(i));
                    if(child!=null){ String cn=child.model.getName(); float tw=textPaint.measureText(cn!=null?cn:"?"); float bw=tw+30;
                        if(i%2==0)maxKeyW=Math.max(maxKeyW,bw); else maxValW=Math.max(maxValW,bw); } }
                float cellW=Math.max(maxKeyW,maxValW)+cellPadding*2;
                for(int i=0;i<pairCount;i++){ float cellX=startX+i*(cellW+6);
                    RectF cell=new RectF(cellX,startY,cellX+cellW,startY+cellH); cellPaint.setColor(0x00000000);cellPaint.setStyle(Paint.Style.FILL);canvas.drawRoundRect(cell,6,6,cellPaint);
                    cellPaint.setColor(0xFFe67e22);cellPaint.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(cell,6,6,cellPaint);
                    smallTextPaint.setColor(0xFFe67e22);canvas.drawText("Key",cellX+6,startY+cellH/2+4,smallTextPaint);
                    int keyIdx=i*2; if(keyIdx<cc){ DrawableBlock child=blocks.get(db.model.getChildrenIds().get(keyIdx));
                        if(child!=null){ child.x=cellX+cellPadding;child.y=startY+cellH+2;child.model.getPosition().put("x",(double)child.x);child.model.getPosition().put("y",(double)child.y);drawBlock(canvas,child); } } }
                float valueY=startY+cellH+8;
                for(int i=0;i<pairCount;i++){ float cellX=startX+i*(cellW+6);
                    RectF cell=new RectF(cellX,valueY,cellX+cellW,valueY+cellH); cellPaint.setColor(0x00000000);cellPaint.setStyle(Paint.Style.FILL);canvas.drawRoundRect(cell,6,6,cellPaint);
                    cellPaint.setColor(0xFF27ae60);cellPaint.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(cell,6,6,cellPaint);
                    smallTextPaint.setColor(0xFF27ae60);canvas.drawText("Value",cellX+4,valueY+cellH/2+4,smallTextPaint);
                    int valIdx=i*2+1; if(valIdx<cc){ DrawableBlock child=blocks.get(db.model.getChildrenIds().get(valIdx));
                        if(child!=null){ child.x=cellX+cellPadding;child.y=valueY+cellH+2;child.model.getPosition().put("x",(double)child.x);child.model.getPosition().put("y",(double)child.y);drawBlock(canvas,child); } } }
            } else if("list".equals(ct)||"array".equals(ct)){
                float cw=100,ch2=h-16; int tc=Math.max(1,cc+1);
                for(int i=0;i<tc;i++){ float cx=db.x+w+10+i*(cw+8),cy=db.y+8; RectF cell=new RectF(cx,cy,cx+cw,cy+ch2);
                    if(i<cc){ DrawableBlock child=blocks.get(db.model.getChildrenIds().get(i)); if(child!=null){ blockPaint.setColor(0x18000000);blockPaint.setStyle(Paint.Style.FILL);canvas.drawRoundRect(cell,8,8,blockPaint); child.x=cx+4;child.y=cy+4;child.model.getPosition().put("x",(double)child.x);child.model.getPosition().put("y",(double)child.y);child.model.getSize().put("width",cw-8.0);child.model.getSize().put("height",ch2-8.0);drawBlock(canvas,child); } }
                    else{ cellPaint.setColor(CELL_COLOR);canvas.drawRoundRect(cell,8,8,cellPaint);cellPaint.setColor(CELL_BORDER_COLOR);cellPaint.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(cell,8,8,cellPaint);cellPaint.setStyle(Paint.Style.FILL);smallTextPaint.setColor(0xFF666666);canvas.drawText("["+i+"]",cx+30,cy+ch2/2+4,smallTextPaint); } }
            } else if("function".equals(ct)||"method".equals(ct)){
                float cw=120,ch2=h-16;
                RectF pc=new RectF(db.x+w+10,db.y+8,db.x+w+10+cw,db.y+8+ch2); cellPaint.setColor(0xFF9b59b6);canvas.drawRoundRect(pc,8,8,cellPaint);cellPaint.setColor(CELL_BORDER_COLOR);cellPaint.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(pc,8,8,cellPaint);cellPaint.setStyle(Paint.Style.FILL);smallTextPaint.setColor(0xFFffffff);canvas.drawText("Params",db.x+w+30,db.y+ch2/2+8,smallTextPaint);
                RectF bc=new RectF(db.x+w+10+cw+8,db.y+8,db.x+w+10+cw*2+8,db.y+8+ch2); cellPaint.setColor(0xFF34495e);canvas.drawRoundRect(bc,8,8,cellPaint);cellPaint.setColor(CELL_BORDER_COLOR);cellPaint.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(bc,8,8,cellPaint);cellPaint.setStyle(Paint.Style.FILL);smallTextPaint.setColor(0xFFffffff);canvas.drawText("Body",db.x+w+cw+30,db.y+ch2/2+8,smallTextPaint);
            } else {
                float cw=100,ch2=h-16; int tc=Math.max(1,cc+1);
                for(int i=0;i<tc;i++){ float cx=db.x+w+10+i*(cw+8),cy=db.y+8; RectF cell=new RectF(cx,cy,cx+cw,cy+ch2);
                    if(i<cc){ DrawableBlock child=blocks.get(db.model.getChildrenIds().get(i)); if(child!=null){ blockPaint.setColor(0x18000000);blockPaint.setStyle(Paint.Style.FILL);canvas.drawRoundRect(cell,8,8,blockPaint); child.x=cx+4;child.y=cy+4;child.model.getPosition().put("x",(double)child.x);child.model.getPosition().put("y",(double)child.y);child.model.getSize().put("width",cw-8.0);child.model.getSize().put("height",ch2-8.0);drawBlock(canvas,child); } }
                    else{ cellPaint.setColor(CELL_COLOR);canvas.drawRoundRect(cell,8,8,cellPaint);cellPaint.setColor(CELL_BORDER_COLOR);cellPaint.setStyle(Paint.Style.STROKE);canvas.drawRoundRect(cell,8,8,cellPaint);cellPaint.setStyle(Paint.Style.FILL);smallTextPaint.setColor(0xFF666666);canvas.drawText("Empty",cx+20,cy+ch2/2+4,smallTextPaint); } }
            }
        }

        if(!db.model.getChildrenIds().isEmpty()&&!db.model.isContainerBlock()){ float cy=db.y+h+8,cx=db.x+CHILD_INDENT;int idx=0;for(int cid:db.model.getChildrenIds()){ DrawableBlock child=blocks.get(cid);if(child!=null){float cw=getBlockWidth(child),ch2=getBlockHeight(child);child.x=cx;child.y=cy;child.model.getPosition().put("x",(double)cx);child.model.getPosition().put("y",(double)cy);blockPaint.setColor(idx%2==0?0x18000000:0x10000000);blockPaint.setStyle(Paint.Style.FILL);canvas.drawRoundRect(new RectF(cx-5,cy-2,cx+cw+40,cy+ch2+2),6,6,blockPaint);smallTextPaint.setColor(0xFF888888);canvas.drawText((idx+1)+".",cx-18,cy+ch2/2+4,smallTextPaint);drawBlock(canvas,child);idx++;cy+=ch2+4;} } }
        if(!db.model.getChildrenIds().isEmpty()){smallTextPaint.setColor(0xFF888888);canvas.drawText("🏠"+db.model.getChildrenIds().size(),db.x+6,db.y+HEADER_HEIGHT-6,smallTextPaint);}
        if(!db.model.getChildrenIds().isEmpty()){boolean col=collapsedBlocks.contains(db.id);buttonPaint.setColor(0xFF3a3a3a);canvas.drawCircle(db.x+w-16,db.y+10,8,buttonPaint);smallTextPaint.setColor(0xFFcccccc);canvas.drawText(col?"+":"−",db.x+w-20,db.y+14,smallTextPaint);}
        if(db.selected)canvas.drawRoundRect(new RectF(rect.left-2,rect.top-2,rect.right+2,rect.bottom+2),BLOCK_RADIUS+2,BLOCK_RADIUS+2,selectionPaint);
        if(db==nestTarget&&draggingBlock!=null&&db!=swapTarget){nestPaint.setColor(nestingReady?NEST_READY_COLOR:Color.argb((int)(80+175*Math.min(1f,(System.currentTimeMillis()-nestHoverStart)/NEST_HOVER_DELAY)),46,204,113));nestPaint.setPathEffect(new DashPathEffect(new float[]{10,5},0));canvas.drawRoundRect(new RectF(rect.left-8,rect.top-8,rect.right+8,rect.bottom+8),BLOCK_RADIUS+8,BLOCK_RADIUS+8,nestPaint);nestPaint.setPathEffect(null);smallTextPaint.setColor(nestingReady?NEST_READY_COLOR:0xFFaaaaaa);canvas.drawText(nestingReady?"▼ RELEASE TO NEST ▼":"▼ HOLD TO NEST ▼",db.x+w/2-80,db.y-12,smallTextPaint);}
        if(db==swapTarget&&draggingBlock!=null){swapPaint.setPathEffect(new DashPathEffect(new float[]{6,3},0));canvas.drawRoundRect(new RectF(rect.left-4,rect.top-4,rect.right+4,rect.bottom+4),BLOCK_RADIUS+4,BLOCK_RADIUS+4,swapPaint);swapPaint.setPathEffect(null);smallTextPaint.setColor(SWAP_COLOR);canvas.drawText("⇄ SWAP",db.x+w/2-25,db.y-8,smallTextPaint);}
    }

    private void drawConnection(Canvas canvas, DrawableBlock from, DrawableBlock to) { float fw=getBlockWidth(from),fh=getBlockHeight(from),tw=getBlockWidth(to),th=getBlockHeight(to); PointF s=new PointF(from.x+fw,from.y+fh/2),e=new PointF(to.x,to.y+th/2); connectionPath.reset();connectionPath.moveTo(s.x,s.y);float dx=e.x-s.x;connectionPath.cubicTo(s.x+dx*0.4f,s.y,e.x-dx*0.4f,e.y,e.x,e.y);canvas.drawPath(connectionPath,connectionPaint); float ang=(float)Math.atan2(e.y-s.y,e.x-s.x),sz=12,sw=6; Path arrow=new Path();arrow.moveTo(e.x,e.y);arrow.lineTo(e.x-sz*(float)Math.cos(ang-0.4f),e.y-sz*(float)Math.sin(ang-0.4f));arrow.lineTo(e.x-sw*(float)Math.cos(ang+Math.PI/2),e.y-sw*(float)Math.sin(ang+Math.PI/2));arrow.lineTo(e.x-sz*(float)Math.cos(ang+0.4f),e.y-sz*(float)Math.sin(ang+0.4f));arrow.close(); connectionPaint.setStyle(Paint.Style.FILL);canvas.drawPath(arrow,connectionPaint);connectionPaint.setStyle(Paint.Style.STROKE); }

    @Override public boolean onTouchEvent(MotionEvent event) {
        float wx=toWorldX(event.getX()),wy=toWorldY(event.getY());
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:
                if(showContextMenu){ String action=getContextMenuAction(event.getX(),event.getY()); if(action!=null&&contextMenuBlock!=null&&"exit".equals(action)&&contextMenuBlock.model.getParentId()!=null){ DrawableBlock parent=blocks.get(contextMenuBlock.model.getParentId()); if(parent!=null){parent.model.getChildrenIds().remove((Integer)contextMenuBlock.id);contextMenuBlock.model.setParentId(null);if(canvasChangeListener!=null)canvasChangeListener.onBlockNested(contextMenuBlock.id,-1);} } hideContextMenu(); return true; }
                long now=System.currentTimeMillis(); if(now-lastClickTime<DOUBLE_CLICK_INTERVAL){ DrawableBlock dblHit=findBlockAt(wx,wy); if(dblHit!=null&&dblHit.model.getParentId()!=null){ showContextMenuForBlock(dblHit,event.getX(),event.getY()); lastClickTime=0; return true; } } lastClickTime=now;
                lastTouchX=event.getX();lastTouchY=event.getY();
                PortHit ph=findPortAt(wx,wy); if(ph!=null){isDraggingConnection=true;connectionSource=ph.block;dragStartPort=ph.port;tempLineStart=getPortPos(ph.block,ph.port);tempLineEnd=new PointF(wx,wy);return true;}
                DrawableBlock hit=findBlockAt(wx,wy); if(hit!=null){if(isCollapseBtn(wx,wy,hit)){toggleCollapse(hit.id);return true;}isDraggingBlock=true;draggingBlock=hit;dragOffsetX=wx-hit.x;dragOffsetY=wy-hit.y;for(DrawableBlock db:blocks.values())db.selected=(db==hit);if(blockClickListener!=null)blockClickListener.onBlockClick(hit.model);return true;}
                isPanning=true;return true;
            case MotionEvent.ACTION_POINTER_DOWN: if(event.getPointerCount()==2){initialSpan=spacing(event);}return true;
            case MotionEvent.ACTION_MOVE: if(event.getPointerCount()==2){float ns=spacing(event);scaleFactor*=ns/initialSpan;scaleFactor=Math.max(ZOOM_MIN,Math.min(ZOOM_MAX,scaleFactor));initialSpan=ns;invalidate();return true;} if(isDraggingConnection){tempLineEnd=new PointF(wx,wy);invalidate();return true;} if(isDraggingBlock&&draggingBlock!=null){draggingBlock.x=clampX(wx-dragOffsetX);draggingBlock.y=clampY(wy-dragOffsetY);draggingBlock.model.getPosition().put("x",(double)draggingBlock.x);draggingBlock.model.getPosition().put("y",(double)draggingBlock.y);checkNesting(draggingBlock,wx,wy);invalidate();return true;} if(isPanning){panX+=event.getX()-lastTouchX;panY+=event.getY()-lastTouchY;clampPan();lastTouchX=event.getX();lastTouchY=event.getY();invalidate();return true;} break;
            case MotionEvent.ACTION_UP: if(isDraggingConnection){PortHit tgt=findPortAt(wx,wy);if(tgt!=null&&connectionSource!=null&&tgt.block!=connectionSource){String fp=dragStartPort,tp=tgt.port;DrawableBlock fb=connectionSource,tb=tgt.block;if("input".equals(fp)&&"output".equals(tp)){DrawableBlock tmp=fb;fb=tb;tb=tmp;fp="output";tp="input";}if("output".equals(fp)&&"input".equals(tp)&&canvasChangeListener!=null)canvasChangeListener.onConnectionCreated(fb.id,fp,tb.id,tp);}resetState();invalidate();return true;} if(isDraggingBlock&&draggingBlock!=null){handleDrop();if(canvasChangeListener!=null)canvasChangeListener.onBlockMoved(draggingBlock.id,draggingBlock.x,draggingBlock.y);}resetState();invalidate();return true;
        }
        return super.onTouchEvent(event);
    }

    private void resetState(){isDraggingBlock=false;isDraggingConnection=false;isPanning=false;draggingBlock=null;connectionSource=null;dragStartPort=null;tempLineStart=null;tempLineEnd=null;nestTarget=null;swapTarget=null;nestingReady=false;}
    private void checkNesting(DrawableBlock dragged,float wx,float wy){nestTarget=null;swapTarget=null;for(DrawableBlock db:blocks.values()){if(db==dragged)continue;float w=getBlockWidth(db),h=getBlockHeight(db);if(wx>=db.x&&wx<=db.x+w&&wy>=db.y&&wy<=db.y+h){if(dragged.model.getParentId()!=null&&db.model.getParentId()!=null&&dragged.model.getParentId().equals(db.model.getParentId())){swapTarget=db;nestingReady=true;return;}if(!wouldCreateCycle(dragged,db)){nestTarget=db;if(nestHoverStart==0||nestTarget!=db){nestHoverStart=System.currentTimeMillis();nestingReady=false;}else if(System.currentTimeMillis()-nestHoverStart>NEST_HOVER_DELAY)nestingReady=true;return;}}}nestHoverStart=0;nestingReady=false;}
    private void handleDrop(){if(swapTarget!=null&&draggingBlock!=null){swapChildren(draggingBlock,swapTarget);}else if(nestTarget!=null&&nestingReady&&draggingBlock!=null){nestBlock(draggingBlock.id,nestTarget.id);}nestTarget=null;swapTarget=null;nestingReady=false;}

    private DrawableBlock findBlockAt(float x,float y){List<DrawableBlock> rev=new ArrayList<>(blocks.values());Collections.reverse(rev);for(DrawableBlock db:rev){if(x>=db.x&&x<=db.x+getBlockWidth(db)&&y>=db.y&&y<=db.y+getBlockHeight(db))return db;}return null;}
    private PortHit findPortAt(float x,float y){for(DrawableBlock db:blocks.values()){float w=getBlockWidth(db),h=getBlockHeight(db);if(Math.hypot(x-db.x,y-(db.y+h/2))<=PORT_RADIUS*2.5f)return new PortHit(db,"input");if(Math.hypot(x-(db.x+w),y-(db.y+h/2))<=PORT_RADIUS*2.5f)return new PortHit(db,"output");}return null;}
    private PointF getPortPos(DrawableBlock db,String port){return "input".equals(port)?new PointF(db.x,db.y+getBlockHeight(db)/2):new PointF(db.x+getBlockWidth(db),db.y+getBlockHeight(db)/2);}
    private boolean isCollapseBtn(float wx,float wy,DrawableBlock db){float cx=db.x+getBlockWidth(db)-16,cy=db.y+10;return Math.hypot(wx-cx,wy-cy)<=12&&!db.model.getChildrenIds().isEmpty();}
    private float spacing(MotionEvent e){float x=e.getX(0)-e.getX(1),y=e.getY(0)-e.getY(1);return (float)Math.sqrt(x*x+y*y);}
    private float getBlockWidth(DrawableBlock db){return db.model.getSize().getOrDefault("width",150.0).floatValue();}
    private float getBlockHeight(DrawableBlock db){return db.model.getSize().getOrDefault("height",80.0).floatValue();}
    private float toWorldX(float sx){return (sx-panX)/scaleFactor;}
    private float toWorldY(float sy){return (sy-panY)/scaleFactor;}
    private int parseColor(String c){try{return Color.parseColor(c);}catch(Exception e){return 0xFF3498db;}}
    private int darken(int c){float f=0.8f;return Color.argb(Color.alpha(c),(int)(Color.red(c)*f),(int)(Color.green(c)*f),(int)(Color.blue(c)*f));}

    public static class DrawableBlock { public BlockModel model; public int id; public float x,y; public boolean selected; }
    public static class DrawableConnection { public Connection connection; public String id; public int fromBlockId, toBlockId; public String fromPort, toPort; }
    private static class PortHit { DrawableBlock block; String port; PortHit(DrawableBlock b,String p){block=b;port=p;} }
    public interface OnBlockClickListener { void onBlockClick(BlockModel block); }
    public interface OnCanvasChangeListener { void onBlockMoved(int blockId, float x, float y); void onConnectionCreated(int fromId, String fromPort, int toId, String toPort); void onBlockNested(int childId, int parentId); }
}