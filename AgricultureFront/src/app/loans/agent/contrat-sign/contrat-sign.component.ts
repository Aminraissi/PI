import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ContratService } from '../../../services/loans/contrat.service';
import { Router } from '@angular/router';
import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';

@Component({
  selector: 'app-contrat-sign',
  templateUrl: './contrat-sign.component.html',
  styleUrls: ['./contrat-sign.component.css']
})
export class ContratSignComponent implements OnInit {

  @ViewChild('canvas', { static: false }) canvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('contractContent', { static: false }) contractContent!: ElementRef<HTMLDivElement>;

  contrat: any = null;
  private ctx!: CanvasRenderingContext2D;
  private drawing = false;
  contratLoaded = false;
  isSaving = false;

  constructor(
    private route: ActivatedRoute,
    private contratService: ContratService,
    private router: Router
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadContrat(+id);
    }
  }

  loadContrat(id: number) {
    this.contratService.getContrat(id).subscribe({
      next: (res: any) => {
        this.contrat = res;
        this.contratLoaded = true;
        setTimeout(() => this.initCanvas(), 200);
      },
      error: (err) => {
        console.error('Error loading contrat:', err);
        this.contratLoaded = false;
      }
    });
  }

  initCanvas() {
    const canvasEl = this.canvas?.nativeElement;
    if (!canvasEl) return;

    this.ctx = canvasEl.getContext('2d')!;
    if (!this.ctx) return;

    canvasEl.width = 500;
    canvasEl.height = 200;

    this.ctx.strokeStyle = '#000';
    this.ctx.lineWidth = 2;
    this.ctx.lineCap = 'round';

    this.ctx.beginPath();

    canvasEl.onmousedown = () => {
      this.drawing = true;
    };

    canvasEl.onmouseup = () => {
      this.drawing = false;
      this.ctx.beginPath();
    };

    canvasEl.onmouseleave = () => {
      this.drawing = false;
      this.ctx.beginPath();
    };

    canvasEl.onmousemove = (event: MouseEvent) => {
      if (!this.drawing) return;
      const rect = canvasEl.getBoundingClientRect();
      const x = event.clientX - rect.left;
      const y = event.clientY - rect.top;
      this.ctx.lineTo(x, y);
      this.ctx.stroke();
      this.ctx.beginPath();
      this.ctx.moveTo(x, y);
    };
  }

  clear() {
    const canvasEl = this.canvas?.nativeElement;
    if (!canvasEl) return;
    this.ctx.clearRect(0, 0, canvasEl.width, canvasEl.height);
    this.ctx.beginPath();
  }

  async save() {
    const canvasEl = this.canvas?.nativeElement;
    if (!canvasEl) return;

    if (!this.contrat?.contrat?.id) {
      alert("Contract not yet loaded");
      return;
    }

    const isCanvasEmpty = this.isCanvasEmpty(canvasEl);
    if (isCanvasEmpty) {
      alert("Please sign the contract before submitting");
      return;
    }

    this.isSaving = true;

    try {
      const signatureBase64 = canvasEl.toDataURL('image/png');

      const pdfBlob = await this.generateContractPDF(signatureBase64);

      this.contratService.signContratWithPDF(
        this.contrat.contrat.id,
        signatureBase64,
        pdfBlob
      ).subscribe({
        next: () => {
          alert('✓ Contract signed and saved successfully!');
          this.isSaving = false;
          setTimeout(() => {
            this.router.navigate(['/']);
          }, 1500);
        },
        error: (error) => {
          console.error("Error saving contract:", error);
          alert("Error: " + (error.error?.error || error.message));
          this.isSaving = false;
        }
      });

    } catch (error) {
      console.error("Error generating PDF:", error);
      alert("Error generating PDF");
      this.isSaving = false;
    }
  }

  isCanvasEmpty(canvas: HTMLCanvasElement): boolean {
    const context = canvas.getContext('2d');
    if (!context) return true;
    
    const imageData = context.getImageData(0, 0, canvas.width, canvas.height);
    const data = imageData.data;
    
    for (let i = 0; i < data.length; i += 4) {
      if (data[i] !== 255 || data[i+1] !== 255 || data[i+2] !== 255) {
        return false;
      }
    }
    return true;
  }

  async generateContractPDF(signatureBase64: string): Promise<Blob> {
    const originalContainer = this.contractContent?.nativeElement;
    
    if (!originalContainer) {
      throw new Error('Contract content not found');
    }
    
    // Créer un clone profond du conteneur original
    const cloneContainer = originalContainer.cloneNode(true) as HTMLElement;
    
    // Cacher le clone hors de l'écran
    cloneContainer.style.position = 'absolute';
    cloneContainer.style.top = '-9999px';
    cloneContainer.style.left = '-9999px';
    cloneContainer.style.width = '800px';
    cloneContainer.style.padding = '40px';
    cloneContainer.style.backgroundColor = 'white';
    cloneContainer.style.zIndex = '-1';
    
    // Ajouter la signature dans la section signature du clone
    const signatureSection = cloneContainer.querySelector('.signature-section');
    if (signatureSection) {
      // Supprimer l'ancienne image de signature si elle existe
      const oldImg = signatureSection.querySelector('img');
      if (oldImg) {
        oldImg.remove();
      }
      
      // Créer la nouvelle image de signature
      const signatureImg = document.createElement('img');
      signatureImg.src = signatureBase64;
      signatureImg.alt = 'Signature';
      signatureImg.style.maxWidth = '250px';
      signatureImg.style.marginTop = '15px';
      signatureImg.style.border = '1px solid #ddd';
      signatureImg.style.padding = '10px';
      signatureImg.style.display = 'block';
      signatureImg.style.marginLeft = 'auto';
      signatureImg.style.marginRight = 'auto';
      
      // Ajouter l'image à la fin de la section signature
      signatureSection.appendChild(signatureImg);
    }
    
    document.body.appendChild(cloneContainer);
    
    // Attendre que le DOM soit prêt
    await new Promise(resolve => setTimeout(resolve, 100));
    
    const canvas = await html2canvas(cloneContainer, {
      scale: 2,
      backgroundColor: '#ffffff',
      logging: false,
      useCORS: true
    });
    
    document.body.removeChild(cloneContainer);
    
    const imgData = canvas.toDataURL('image/jpeg', 0.8);
    const pdf = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4'
    });
    
    const imgWidth = 190;
    const pageHeight = 277;
    const imgHeight = (canvas.height * imgWidth) / canvas.width;
    
    pdf.addImage(imgData, 'JPEG', 10, 10, imgWidth, imgHeight);
    
    let heightLeft = imgHeight;
    let position = 0;
    while (heightLeft > pageHeight) {
      position = heightLeft - pageHeight;
      pdf.addPage();
      pdf.addImage(imgData, 'JPEG', 10, -position, imgWidth, imgHeight);
      heightLeft -= pageHeight;
    }
    
    const pdfBlob = pdf.output('blob');
    console.log('PDF generated, size:', pdfBlob.size, 'bytes');
    
    return pdfBlob;
  }

  calculateMensualite(montant: number, taux: number, duree: number): number {
    if (!montant || !taux || !duree) return 0;
    const interet = (montant * taux / 100);
    const total = montant + interet;
    return total / duree;
  }
}