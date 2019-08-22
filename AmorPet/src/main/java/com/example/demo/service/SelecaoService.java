package com.example.demo.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.dao.AnimalDAO;
import com.example.demo.dao.ProcessoDAO;
import com.example.demo.dao.SelecaoDAO;
import com.example.demo.dao.UsuarioDAO;
import com.example.demo.model.Animal;
import com.example.demo.model.Processo;
import com.example.demo.model.Resposta;
import com.example.demo.model.Selecao;
import com.example.demo.model.Usuario;

@Service
public class SelecaoService {
	@Autowired
	private SelecaoDAO selecaoRep;
	@Autowired
	private AnimalDAO animalRep;
	@Autowired
	private ProcessoDAO processoRep;
	@Autowired
	private UsuarioDAO usuarioRep;

	public List<Selecao> lista() {
		return this.selecaoRep.findAll();
	}

	public Selecao findById(Integer id) throws Exception {
		Optional<Selecao> selecao = this.selecaoRep.findById(id);
		if (selecao.isPresent()) {
			return selecao.get();
		}
		throw new Exception("Seleção não encontrada");
	}

	/* CRIA UMA SELECAO OU USA A EXISTENTE */
	public void selecaoAnimal(Integer idAnimal, Processo processo) throws Exception {

		// verificar se o animal existe
		Animal animal = this.animalRep.findById(idAnimal).orElseThrow(Exception::new);

		Selecao selecao = this.findBySelecao(animal);

		// caso a seleção exista no bd
		if (selecao.getIdSelecao() != null) {

			for (Processo p : selecao.getProcessos()) {
				// se o usuario já estiver no processo da selecao
				if (p.getIdUsuario().getId().equals(processo.getIdUsuario().getId())) {
					return;
				}
			}
		}
		this.selecaoRep.save(selecao);
		selecao = this.findBySelecao(animal);

		processo.setIdSelecao(selecao);
		this.processoRep.save(processo);

		selecao.getProcessos().add(processo);
		this.selecaoRep.save(selecao);
	}

	/* VERIFICAR SE EXISTE UMA SELECAO ABERTA OU CRIA UMA NOVA */
	public Selecao findBySelecao(Animal animal) {
		Optional<Selecao> selecao = this.selecaoRep.findByIdAnimal(animal.getIdAnimal());
		if (selecao.isPresent()) {
			// verifica se a selecao está aberta
			if (selecao.get().getSituacao() == 1) {
				return selecao.get();
			}
		}
		// cria uma nova selecao caso não tenha selecao aberta
		Selecao novaSelecao = this.novaSelecao(animal);
		return novaSelecao;
	}

	/* CRIA UMA NOVA SELECAO */
	public Selecao novaSelecao(Animal animal) {
		Selecao selecao = new Selecao();
		selecao.setIdAnimal(animal);
		selecao.setDataRegistro(LocalDate.now());
		selecao.setProcessos(new ArrayList<Processo>());
		selecao.setSituacao(1);
		return selecao;
	}
	
	public void verificarPosicaoProcesso(Selecao selecao, Integer idProcesso) throws Exception {
		Optional<Processo> processo = this.processoRep.findById(idProcesso);
		if(processo.isPresent()) {
			int i = 0;
			for (Processo p : selecao.getProcessos()) {
				if(i > 5) {
					throw new Exception("Processo não têm uma colocação mínina");
				}
				if(p.getIdProcesso().equals(processo.get().getIdProcesso())) {
					break;
				}
				i++;
			}
			if(selecao.getSituacao() == 1) {
				if(i <= 5) {
					return;
				}
			}else if(selecao.getSituacao() == 2) {
				if(i <= 3) {
					return;
				}
			}
		}
		throw new Exception("Processo não existe");
	}

	public List<Resposta> buscaResposta(Selecao selecao, Integer idProcesso) throws Exception {
		Optional<Processo> processo = this.processoRep.findById(idProcesso);

		if (processo.isPresent()) {
			if(!processo.get().getIdUsuario().isAtivo()) {
				throw new Exception("conta do Usuário desativada");
			}
			if (selecao.getProcessos().contains(processo.get())) {
				return processo.get().getRespostas();
			}
		}
		throw new Exception("Não existe");
	}

	public void iniciarProximaEtapa(Integer idSelecao, Integer etapa) throws Exception {
		Selecao selecao = this.findById(idSelecao);
		selecao.setSituacao(etapa);
		this.selecaoRep.save(selecao);
	}

	public void finalizarSelecao(Selecao selecao) throws Exception {
		if (selecao.getSituacao() != 3) {
			throw new Exception("Seleção não está na etapa 3");
		}
		// soma os pontos da avaliacao
		this.somarPontosAvaliacao(selecao);
		// pega todos os processos que tem a pontuacao minima requerida
		List<Processo> processosFinais = this.processoRep.findByProcessosFinalistas(selecao);
		if (processosFinais.isEmpty()) {
			throw new Exception("Não houve participantes aptos para esta seleção");
		}
		Optional<Usuario> usuario;
		// itera na lista verificando se o usuario está ativo para receber o animal
		for (Processo processo : processosFinais) {
			usuario = this.usuarioRep.findById(processo.getIdUsuario().getId());
			if (usuario.isPresent()) {
				usuario.get().getAnimais().add(selecao.getIdAnimal());
				this.usuarioRep.save(usuario.get());
				break;
			}
		}
	}

	public void somarPontosAvaliacao(Selecao selecao) {
		for (Processo p : selecao.getProcessos()) {
			if (p.getAvaliacao().getAvaliacaoDono() == null) {
				continue;
			}
			p.setPontuacaoFinal(p.getAvaliacao().getAvaliacaoDono() + p.getAvaliacao().getAvaliacaoLar());
			this.processoRep.save(p);
		}
	}
}